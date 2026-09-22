package com.fooddelivery.courier.repository;

import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import com.fooddelivery.auth.repository.UserRepository;
import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.entity.CourierStatus;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a courier is shown to have earned.
 *
 * <p>The queries counted {@code status = DELIVERED} only, and the lifecycle
 * scheduler turns a delivered order into a completed one an hour later — so a
 * courier's earnings and delivery count drained away over a shift while the
 * money itself was never wrong. That is invisible in a unit test with a mocked
 * repository, because the bug is the predicate.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:courierearn;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("Courier earnings queries")
class CourierEarningsQueryTest {

    @Autowired private OrderRepository orderRepository;
    @Autowired private CourierRepository courierRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private UserRepository userRepository;

    private Courier courier;
    private Restaurant restaurant;
    private User consumer;
    private int orderSeq;

    @BeforeEach
    void setUp() {
        User courierUser = userRepository.save(User.builder()
                .email("courier@example.com").passwordHash("x")
                .roles(Set.of(Role.COURIER)).status(UserStatus.ACTIVE).build());
        consumer = userRepository.save(User.builder()
                .email("eater@example.com").passwordHash("x")
                .roles(Set.of(Role.CONSUMER)).status(UserStatus.ACTIVE).build());
        User owner = userRepository.save(User.builder()
                .email("owner@example.com").passwordHash("x")
                .roles(Set.of(Role.RESTAURANT_OWNER)).status(UserStatus.ACTIVE).build());

        courier = courierRepository.save(Courier.builder()
                .user(courierUser).status(CourierStatus.AVAILABLE).build());
        restaurant = restaurantRepository.save(Restaurant.builder()
                .owner(owner).name("Osh Markazi").slug("osh-markazi").build());
    }

    private void delivered(OrderStatus status, String fee, String tip, LocalDateTime deliveredAt) {
        Order order = new Order();
        order.setExternalOrderNo("FD-TEST-" + (++orderSeq));
        order.setConsumer(consumer);
        order.setRestaurant(restaurant);
        order.setCourier(courier);
        order.setStatus(status);
        order.setOrderType(com.fooddelivery.order.entity.OrderType.DELIVERY);
        order.setDeliveryFee(new BigDecimal(fee));
        order.setTipAmount(new BigDecimal(tip));
        order.setSubtotal(new BigDecimal("50000"));
        order.setTotal(new BigDecimal("50000"));
        order.setDeliveredAt(deliveredAt);
        orderRepository.save(order);
    }

    @Test
    @DisplayName("a delivery still counts after the order auto-completes")
    void completedOrdersStillCount() {
        // The bug: DELIVERED becomes COMPLETED an hour later, and the courier's
        // screen quietly went back to zero.
        LocalDateTime anHourAgo = LocalDateTime.now().minusHours(1);
        delivered(OrderStatus.COMPLETED, "15000", "5000", anHourAgo);

        assertThat(orderRepository.sumCourierEarningsSince(courier.getId(), anHourAgo.minusHours(1)))
                .isEqualByComparingTo("20000");
        assertThat(orderRepository.countDeliveriesByCourierSince(courier.getId(), anHourAgo.minusHours(1)))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("a just-delivered order counts too")
    void deliveredOrdersCount() {
        LocalDateTime now = LocalDateTime.now();
        delivered(OrderStatus.DELIVERED, "15000", "0", now);

        assertThat(orderRepository.sumCourierEarningsSince(courier.getId(), now.minusHours(1)))
                .isEqualByComparingTo("15000");
    }

    @Test
    @DisplayName("earnings are the delivery fee plus the tip")
    void feePlusTip() {
        LocalDateTime now = LocalDateTime.now();
        delivered(OrderStatus.DELIVERED, "12000", "3000", now);
        delivered(OrderStatus.COMPLETED, "15000", "0", now);

        assertThat(orderRepository.sumCourierEarningsSince(courier.getId(), now.minusHours(1)))
                .isEqualByComparingTo("30000");
    }

    @Test
    @DisplayName("an order that never arrived earns nothing")
    void undeliveredEarnsNothing() {
        delivered(OrderStatus.IN_TRANSIT, "15000", "5000", null);
        delivered(OrderStatus.CANCELLED, "15000", "5000", null);

        assertThat(orderRepository.sumCourierEarningsTotal(courier.getId()))
                .isEqualByComparingTo("0");
        assertThat(orderRepository.countDeliveriesByCourier(courier.getId())).isZero();
    }

    @Test
    @DisplayName("the lifetime total is the same definition, without a window")
    void lifetimeMatchesTheDefinition() {
        delivered(OrderStatus.COMPLETED, "10000", "1000", LocalDateTime.now().minusDays(40));
        delivered(OrderStatus.DELIVERED, "12000", "0", LocalDateTime.now());

        assertThat(orderRepository.sumCourierEarningsTotal(courier.getId()))
                .isEqualByComparingTo("23000");
        assertThat(orderRepository.countDeliveriesByCourier(courier.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("the window still excludes what falls outside it")
    void windowStillApplies() {
        delivered(OrderStatus.COMPLETED, "10000", "0", LocalDateTime.now().minusDays(40));
        delivered(OrderStatus.DELIVERED, "12000", "0", LocalDateTime.now());

        assertThat(orderRepository.sumCourierEarningsSince(
                courier.getId(), LocalDateTime.now().minusDays(1)))
                .isEqualByComparingTo("12000");
    }
}
