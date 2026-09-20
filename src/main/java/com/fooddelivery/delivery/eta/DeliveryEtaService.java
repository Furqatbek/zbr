package com.fooddelivery.delivery.eta;

import com.fooddelivery.courier.entity.VehicleType;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.service.RouteDistanceService;
import com.fooddelivery.restaurant.entity.Restaurant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * What the customer is actually waiting for: the kitchen, then the road.
 *
 * <p>Until now the customer app had only the restaurant's average preparation
 * time, and an order's estimated delivery time was that number plus a flat
 * fifteen minutes — the same fifteen for a courier walking two streets and a
 * courier riding six kilometres across the river. This puts the distance and
 * the vehicle into it.
 *
 * <h2>Two ways to ask, on purpose</h2>
 *
 * <p>{@link #browsing} is straight-line arithmetic with a detour factor, and
 * touches nothing outside this process. A list of thirty restaurant cards is
 * thirty estimates, and thirty OSRM round trips to draw one screen is not a
 * trade anyone would make for a number the customer reads as "about half an
 * hour".
 *
 * <p>{@link #quote} asks the router, because by then there is one restaurant,
 * the customer is about to pay, and the number turns into a promise.
 *
 * <h2>What it deliberately does not model</h2>
 *
 * <p>The courier's journey <em>to</em> the restaurant. Before assignment there
 * is no courier to measure it from, and after assignment the pickup usually
 * overlaps the cooking. Ignoring it makes the estimate slightly pessimistic,
 * which is the safe direction: a customer told 40 minutes and served in 35 is
 * happy, and the reverse is a support call.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryEtaService {

    /** Matches the default on {@code Restaurant.averagePrepTimeMinutes}. */
    static final int DEFAULT_PREP_MINUTES = 30;

    private final RouteDistanceService routeDistanceService;
    private final EtaProperties properties;

    /**
     * For a restaurant card or a list of them: no network, no router.
     *
     * @return empty when either end has no coordinates — the caller leaves the
     *         fields off rather than showing a number it made up.
     */
    public Optional<DeliveryEta> browsing(Restaurant restaurant,
                                          BigDecimal customerLat,
                                          BigDecimal customerLng) {
        if (restaurant == null) {
            return Optional.empty();
        }
        return browsing(restaurant.getLatitude(), restaurant.getLongitude(),
                restaurant.getAveragePrepTimeMinutes(), customerLat, customerLng);
    }

    /**
     * The same, from a DTO that already carries the restaurant's coordinates —
     * so a list of cards can be stamped without loading the entities again.
     */
    public Optional<DeliveryEta> browsing(BigDecimal restaurantLat,
                                          BigDecimal restaurantLng,
                                          Integer prepMinutes,
                                          BigDecimal customerLat,
                                          BigDecimal customerLng) {
        if (restaurantLat == null || restaurantLng == null || customerLat == null || customerLng == null) {
            return Optional.empty();
        }
        double straightLine = routeDistanceService.calculateHaversineDistanceKm(
                restaurantLat.doubleValue(), restaurantLng.doubleValue(),
                customerLat.doubleValue(), customerLng.doubleValue());

        return Optional.of(build(straightLine, false, prepOrDefault(prepMinutes),
                properties.getPlanningVehicle(), true));
    }

    /**
     * For the one restaurant the customer is ordering from: the real route when
     * routing is switched on.
     */
    public Optional<DeliveryEta> quote(Restaurant restaurant,
                                       BigDecimal customerLat,
                                       BigDecimal customerLng) {
        return quote(restaurant, customerLat, customerLng, null, null);
    }

    /**
     * @param vehicle      the courier's vehicle, or null when none is assigned
     *                     and the configured planning vehicle should be assumed
     * @param prepOverride the kitchen's own estimate, or null for the
     *                     restaurant's average
     */
    public Optional<DeliveryEta> quote(Restaurant restaurant,
                                       BigDecimal customerLat,
                                       BigDecimal customerLng,
                                       VehicleType vehicle,
                                       Integer prepOverride) {
        if (!hasCoordinates(restaurant, customerLat, customerLng)) {
            return Optional.empty();
        }
        RouteDistanceService.Distance distance = routeDistanceService.calculateDistance(
                restaurant.getLatitude().doubleValue(),
                restaurant.getLongitude().doubleValue(),
                customerLat.doubleValue(),
                customerLng.doubleValue());

        int prep = prepOverride != null && prepOverride >= 0 ? prepOverride : prepMinutesFor(restaurant);
        return Optional.of(fromDistance(distance.km(), distance.routed(), prep, vehicle));
    }

    /**
     * For a caller that has already measured the distance — the delivery-fee
     * quote, which needs the same journey and must not pay for it twice.
     *
     * @param routed  true when {@code distanceKm} came from the road network,
     *                so the detour factor is not applied again
     * @param vehicle the courier's vehicle, or null to assume the planning one
     */
    public DeliveryEta fromDistance(double distanceKm, boolean routed,
                                    Integer prepMinutes, VehicleType vehicle) {
        return build(distanceKm, routed, prepOrDefault(prepMinutes),
                vehicle != null ? vehicle : properties.getPlanningVehicle(),
                vehicle == null);
    }

    /**
     * For an order in flight, using the assigned courier's actual vehicle.
     *
     * <p>Once the kitchen says READY the prep time is spent, so it drops out of
     * the estimate rather than being counted again against a customer who has
     * already waited it.
     */
    public Optional<DeliveryEta> forOrder(Order order) {
        Restaurant restaurant = order.getRestaurant();
        if (restaurant == null) {
            return Optional.empty();
        }
        VehicleType vehicle = order.getCourier() != null ? order.getCourier().getVehicleType() : null;
        Integer prep = remainingPrepMinutes(order);

        return quote(restaurant, order.getDeliveryLatitude(), order.getDeliveryLongitude(), vehicle, prep);
    }

    /**
     * Recompute when this order's food should arrive, and stamp it on the order.
     *
     * <p>This replaced the kitchen's estimate plus a flat fifteen minutes — the
     * same fifteen for a courier walking two streets and one riding six
     * kilometres across the river. Called again when a courier accepts, because
     * that is the moment the vehicle stops being an assumption.
     *
     * <p>The <em>upper</em> bound is what gets stored: this single timestamp is
     * what the customer is shown counting down, and a late order costs more
     * than an early one. An order with no delivery coordinates — a takeaway, or
     * an address that would not geocode — keeps whatever estimate it had rather
     * than being given an invented one.
     */
    public void refreshEstimatedDeliveryTime(Order order) {
        forOrder(order).ifPresent(eta ->
                order.setEstimatedDeliveryTime(LocalDateTime.now().plusMinutes(eta.getEtaMinutesMax())));
    }

    /**
     * Zero once the food is made or the courier has it; otherwise the kitchen's
     * own estimate if it gave one, else the restaurant's average.
     */
    private Integer remainingPrepMinutes(Order order) {
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.READY || status == OrderStatus.PICKED_UP
                || status == OrderStatus.IN_TRANSIT) {
            return 0;
        }
        return order.getEstimatedPrepTimeMinutes();
    }

    private int prepMinutesFor(Restaurant restaurant) {
        return prepOrDefault(restaurant.getAveragePrepTimeMinutes());
    }

    private int prepOrDefault(Integer prepMinutes) {
        return prepMinutes != null && prepMinutes >= 0 ? prepMinutes : DEFAULT_PREP_MINUTES;
    }

    private boolean hasCoordinates(Restaurant restaurant, BigDecimal lat, BigDecimal lng) {
        return restaurant != null
                && restaurant.getLatitude() != null && restaurant.getLongitude() != null
                && lat != null && lng != null;
    }

    private DeliveryEta build(double rawDistanceKm, boolean routed, int prepMinutes,
                              VehicleType vehicle, boolean vehicleAssumed) {
        // The detour factor approximates the roads. A route from OSRM has
        // already been down them, so inflating it again would add 30% to every
        // routed estimate.
        double distanceKm = routed ? rawDistanceKm : rawDistanceKm * properties.getDetourFactor();

        int travelMinutes = (int) Math.ceil(distanceKm / properties.speedFor(vehicle) * 60.0)
                + properties.overheadFor(vehicle);
        int total = prepMinutes + travelMinutes;

        int spread = Math.max(properties.getMinSpreadMinutes(),
                (int) Math.round(total * properties.getSpreadFraction()));
        int step = properties.getRoundToMinutes();

        return DeliveryEta.builder()
                .distanceKm(Math.round(distanceKm * 100.0) / 100.0)
                .prepMinutes(prepMinutes)
                .travelMinutes(travelMinutes)
                // Rounded outward: a range the app can show as "35-45 min"
                // without implying we know the minute. Never below one step, so
                // nothing is ever quoted as arriving in zero minutes.
                .etaMinutesMin(Math.max(step, roundDownTo(total - spread, step)))
                .etaMinutesMax(roundUpTo(total + spread, step))
                .vehicle(vehicle)
                .vehicleAssumed(vehicleAssumed)
                .routed(routed)
                .build();
    }

    private static int roundDownTo(int minutes, int step) {
        return Math.max(0, minutes) / step * step;
    }

    private static int roundUpTo(int minutes, int step) {
        return (Math.max(0, minutes) + step - 1) / step * step;
    }
}
