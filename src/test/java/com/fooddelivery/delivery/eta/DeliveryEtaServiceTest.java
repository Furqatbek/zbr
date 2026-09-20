package com.fooddelivery.delivery.eta;

import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.entity.VehicleType;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.service.RouteDistanceService;
import com.fooddelivery.restaurant.entity.Restaurant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

/**
 * Distance and arrival time.
 *
 * <p>The numbers below are written out longhand rather than recomputed from the
 * properties, for the reason the partner status table taught us: an expectation
 * derived from the code under test cannot contradict it. If a default speed
 * changes, these fail — which is the point, because every one of them is a
 * number a customer reads.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Delivery ETA")
class DeliveryEtaServiceTest {

    private static final BigDecimal REST_LAT = new BigDecimal("41.311081");
    private static final BigDecimal REST_LNG = new BigDecimal("69.240562");
    private static final BigDecimal CUST_LAT = new BigDecimal("41.326000");
    private static final BigDecimal CUST_LNG = new BigDecimal("69.280000");

    @Mock private RouteDistanceService routeDistanceService;

    private EtaProperties properties;
    private DeliveryEtaService service;

    @BeforeEach
    void setUp() {
        properties = new EtaProperties();
        service = new DeliveryEtaService(routeDistanceService, properties);
    }

    private Restaurant restaurant(Integer prepMinutes) {
        return Restaurant.builder()
                .id(100L)
                .latitude(REST_LAT)
                .longitude(REST_LNG)
                .averagePrepTimeMinutes(prepMinutes)
                .build();
    }

    // ---------------------------------------------------------------------
    // The vehicle table. Five kilometres straight-line, which the detour
    // factor turns into 6.5 km of road, and no kitchen time — so what is left
    // is the courier's share alone.
    // ---------------------------------------------------------------------

    private static int expectedTravelMinutesOver5Km(VehicleType vehicle) {
        return switch (vehicle) {
            // 6.5 km at 4.5 km/h = 87 min, plus 3 to hand it over
            case WALKING -> 90;
            // 6.5 at 12 = 33, plus 3
            case BICYCLE -> 36;
            // 6.5 at 18 = 22, plus 3
            case E_BIKE -> 25;
            // 6.5 at 24 = 17, plus 4
            case MOTORCYCLE -> 21;
            // 6.5 at 20 = 20, plus 6 to park it. Slower than the e-bike, which
            // is the whole reason the vehicle is in this calculation.
            case CAR -> 26;
            // 6.5 at 18 = 22, plus 7
            case VAN -> 29;
        };
    }

    @ParameterizedTest
    @EnumSource(VehicleType.class)
    @DisplayName("every vehicle has its own travel time over the same distance")
    void everyVehicleIsPinned(VehicleType vehicle) {
        DeliveryEta eta = service.fromDistance(5.0, false, 0, vehicle);

        assertThat(eta.getTravelMinutes()).isEqualTo(expectedTravelMinutesOver5Km(vehicle));
        assertThat(eta.getVehicle()).isEqualTo(vehicle);
        assertThat(eta.isVehicleAssumed()).isFalse();
    }

    @Test
    @DisplayName("a courier on foot is quoted longer than one on a motorcycle")
    void slowVehiclesTakeLonger() {
        // The property the table above exists to guarantee, stated on its own
        // so that a table edited into nonsense still fails something.
        DeliveryEta walking = service.fromDistance(5.0, false, 30, VehicleType.WALKING);
        DeliveryEta motorcycle = service.fromDistance(5.0, false, 30, VehicleType.MOTORCYCLE);

        assertThat(walking.getEtaMinutesMax()).isGreaterThan(motorcycle.getEtaMinutesMax());
        assertThat(walking.getEtaMinutesMin()).isGreaterThan(motorcycle.getEtaMinutesMin());
    }

    @Test
    @DisplayName("further away is quoted as longer")
    void distanceMovesTheEstimate() {
        DeliveryEta near = service.fromDistance(1.0, false, 30, VehicleType.BICYCLE);
        DeliveryEta far = service.fromDistance(6.0, false, 30, VehicleType.BICYCLE);

        assertThat(far.getEtaMinutesMax()).isGreaterThan(near.getEtaMinutesMax());
        assertThat(far.getDistanceKm()).isGreaterThan(near.getDistanceKm());
    }

    @Test
    @DisplayName("a straight line is inflated for the roads; a route is not inflated twice")
    void detourFactorAppliesOnlyToStraightLines() {
        DeliveryEta straight = service.fromDistance(5.0, false, 0, VehicleType.BICYCLE);
        DeliveryEta routed = service.fromDistance(5.0, true, 0, VehicleType.BICYCLE);

        // 5 km as the crow flies is 6.5 km of road; 5 km of road is 5 km.
        assertThat(straight.getDistanceKm()).isEqualTo(6.5);
        assertThat(routed.getDistanceKm()).isEqualTo(5.0);
        assertThat(straight.isRouted()).isFalse();
        assertThat(routed.isRouted()).isTrue();
    }

    @Test
    @DisplayName("the kitchen's time is part of the wait")
    void prepTimeIsIncluded() {
        DeliveryEta quick = service.fromDistance(5.0, false, 10, VehicleType.MOTORCYCLE);
        DeliveryEta slow = service.fromDistance(5.0, false, 40, VehicleType.MOTORCYCLE);

        assertThat(slow.getPrepMinutes()).isEqualTo(40);
        assertThat(slow.getEtaMinutesMin()).isGreaterThan(quick.getEtaMinutesMin());
        assertThat(slow.getEtaMinutesMax()).isGreaterThan(quick.getEtaMinutesMax());
    }

    @Test
    @DisplayName("the quoted range is rounded outward to five minutes")
    void rangeIsRoundedOutward() {
        // 30 min kitchen + 21 min courier = 51, spread 8 -> 43..59, rounded out.
        DeliveryEta eta = service.fromDistance(5.0, false, 30, VehicleType.MOTORCYCLE);

        assertThat(eta.getEtaMinutesMin()).isEqualTo(40);
        assertThat(eta.getEtaMinutesMax()).isEqualTo(60);
    }

    @Test
    @DisplayName("nothing is ever quoted as arriving in zero minutes")
    void neverQuotesZero() {
        DeliveryEta eta = service.fromDistance(0.0, true, 0, VehicleType.WALKING);

        assertThat(eta.getEtaMinutesMin()).isGreaterThanOrEqualTo(5);
        assertThat(eta.getEtaMinutesMax()).isGreaterThan(0);
    }

    // ---------------------------------------------------------------------
    // Missing coordinates. The app shows nothing rather than a wrong number.
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("no customer location means no estimate, not a zero-distance one")
    void noCustomerLocationMeansNoEstimate() {
        assertThat(service.browsing(restaurant(30), null, null)).isEmpty();
        assertThat(service.quote(restaurant(30), null, CUST_LNG)).isEmpty();
    }

    @Test
    @DisplayName("a restaurant with no coordinates gets no estimate")
    void noRestaurantLocationMeansNoEstimate() {
        Restaurant unplaced = Restaurant.builder().id(101L).averagePrepTimeMinutes(30).build();

        assertThat(service.browsing(unplaced, CUST_LAT, CUST_LNG)).isEmpty();
        assertThat(service.quote(unplaced, CUST_LAT, CUST_LNG)).isEmpty();
    }

    @Test
    @DisplayName("browsing never calls the router")
    void browsingDoesNotRoute() {
        when(routeDistanceService.calculateHaversineDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(3.0);

        Optional<DeliveryEta> eta = service.browsing(restaurant(30), CUST_LAT, CUST_LNG);

        // Thirty cards on a screen must not be thirty round trips to OSRM.
        assertThat(eta).isPresent();
        org.mockito.Mockito.verify(routeDistanceService, org.mockito.Mockito.never())
                .calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble());
        assertThat(eta.get().isRouted()).isFalse();
    }

    @Test
    @DisplayName("a quote asks the router, and says so when it answered")
    void quoteRoutes() {
        when(routeDistanceService.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new RouteDistanceService.Distance(4.2, true));

        Optional<DeliveryEta> eta = service.quote(restaurant(30), CUST_LAT, CUST_LNG);

        assertThat(eta).isPresent();
        assertThat(eta.get().isRouted()).isTrue();
        assertThat(eta.get().getDistanceKm()).isEqualTo(4.2);
    }

    // ---------------------------------------------------------------------
    // Which vehicle, and when it stops being a guess.
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("with no courier assigned the estimate says it assumed the vehicle")
    void unassignedOrdersAssumeThePlanningVehicle() {
        properties.setPlanningVehicle(VehicleType.E_BIKE);
        when(routeDistanceService.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new RouteDistanceService.Distance(4.0, true));

        DeliveryEta eta = service.quote(restaurant(30), CUST_LAT, CUST_LNG).orElseThrow();

        // The flag is what lets a caller decide whether the number is worth
        // showing to the minute. Silently assuming is how a guess becomes a
        // promise.
        assertThat(eta.getVehicle()).isEqualTo(VehicleType.E_BIKE);
        assertThat(eta.isVehicleAssumed()).isTrue();
    }

    @Test
    @DisplayName("an assigned courier's own vehicle replaces the assumption")
    void assignedOrdersUseTheRealVehicle() {
        properties.setPlanningVehicle(VehicleType.BICYCLE);
        when(routeDistanceService.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new RouteDistanceService.Distance(5.0, true));
        Order order = order(OrderStatus.COURIER_ASSIGNED, VehicleType.CAR, 20);

        DeliveryEta eta = service.forOrder(order).orElseThrow();

        assertThat(eta.getVehicle()).isEqualTo(VehicleType.CAR);
        assertThat(eta.isVehicleAssumed()).isFalse();
    }

    @Test
    @DisplayName("once the food is ready the kitchen's time stops being counted")
    void readyOrdersDropThePrepTime() {
        when(routeDistanceService.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new RouteDistanceService.Distance(5.0, true));

        DeliveryEta cooking = service.forOrder(order(OrderStatus.PREPARING, VehicleType.BICYCLE, 25)).orElseThrow();
        DeliveryEta ready = service.forOrder(order(OrderStatus.READY, VehicleType.BICYCLE, 25)).orElseThrow();

        // Counting it again would have the customer waiting out the cooking
        // twice, on a timer, after it had already happened.
        assertThat(cooking.getPrepMinutes()).isEqualTo(25);
        assertThat(ready.getPrepMinutes()).isZero();
        assertThat(ready.getEtaMinutesMax()).isLessThan(cooking.getEtaMinutesMax());
    }

    @Test
    @DisplayName("an order in transit is not still waiting for the kitchen")
    void inTransitOrdersDropThePrepTime() {
        when(routeDistanceService.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new RouteDistanceService.Distance(5.0, true));

        assertThat(service.forOrder(order(OrderStatus.PICKED_UP, VehicleType.BICYCLE, 25))
                .orElseThrow().getPrepMinutes()).isZero();
        assertThat(service.forOrder(order(OrderStatus.IN_TRANSIT, VehicleType.BICYCLE, 25))
                .orElseThrow().getPrepMinutes()).isZero();
    }

    // ---------------------------------------------------------------------
    // Stamping the order.
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("refreshing an order sets an arrival time in the future")
    void refreshStampsTheOrder() {
        when(routeDistanceService.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new RouteDistanceService.Distance(5.0, true));
        Order order = order(OrderStatus.ACCEPTED, VehicleType.MOTORCYCLE, 30);

        service.refreshEstimatedDeliveryTime(order);

        assertThat(order.getEstimatedDeliveryTime()).isAfter(LocalDateTime.now().plusMinutes(20));
    }

    @Test
    @DisplayName("an order with no delivery coordinates keeps whatever estimate it had")
    void refreshLeavesUnlocatableOrdersAlone() {
        Order order = order(OrderStatus.ACCEPTED, VehicleType.MOTORCYCLE, 30);
        order.setDeliveryLatitude(null);
        order.setDeliveryLongitude(null);
        LocalDateTime existing = LocalDateTime.now().plusMinutes(45);
        order.setEstimatedDeliveryTime(existing);

        service.refreshEstimatedDeliveryTime(order);

        // Not nulled, and not replaced by a number we could not compute.
        assertThat(order.getEstimatedDeliveryTime()).isEqualTo(existing);
    }

    @Test
    @DisplayName("a walking courier's order is stamped later than a motorcycle's")
    void theStampedTimeDependsOnTheVehicle() {
        when(routeDistanceService.calculateDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new RouteDistanceService.Distance(5.0, true));
        Order walking = order(OrderStatus.COURIER_ASSIGNED, VehicleType.WALKING, 30);
        Order riding = order(OrderStatus.COURIER_ASSIGNED, VehicleType.MOTORCYCLE, 30);

        service.refreshEstimatedDeliveryTime(walking);
        service.refreshEstimatedDeliveryTime(riding);

        // The whole point of the change: the flat fifteen minutes this replaced
        // would have stamped these two identically.
        assertThat(walking.getEstimatedDeliveryTime()).isAfter(riding.getEstimatedDeliveryTime());
    }

    private Order order(OrderStatus status, VehicleType vehicle, Integer prepMinutes) {
        Order order = new Order();
        order.setId(5L);
        order.setStatus(status);
        order.setRestaurant(restaurant(prepMinutes));
        order.setDeliveryLatitude(CUST_LAT);
        order.setDeliveryLongitude(CUST_LNG);
        order.setCourier(Courier.builder().id(9L).vehicleType(vehicle).build());
        return order;
    }
}
