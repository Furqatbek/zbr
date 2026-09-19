package com.fooddelivery.analytics.financial;

import com.fooddelivery.analytics.financial.model.RestaurantCommission;
import com.fooddelivery.analytics.financial.repository.RestaurantCommissionRepository;
import com.fooddelivery.auth.entity.Role;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.entity.UserStatus;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.entity.OrderType;
import com.fooddelivery.restaurant.entity.Restaurant;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The service fee, as the financial report sees it.
 *
 * <p>The query joins through the commission row rather than reading orders
 * directly, so it counts exactly the orders GMV and commission count. That is
 * the whole point and it is a string, so only a real database proves it: a
 * revenue line on a different denominator would never reconcile with the
 * others, and nobody could say which of them was wrong.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:servicefee;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("Service fee revenue")
class ServiceFeeRevenueTest {

    private static final LocalDateTime PERIOD_START = LocalDateTime.of(2026, 9, 1, 0, 0);
    private static final LocalDateTime PERIOD_END = LocalDateTime.of(2026, 9, 30, 23, 59);

    @Autowired private RestaurantCommissionRepository repository;
    @Autowired private EntityManager em;

    private Restaurant restaurant;
    private User consumer;

    @BeforeEach
    void setUp() {
        User owner = User.builder().phone("998901234500")
                .role(Role.RESTAURANT_OWNER).status(UserStatus.ACTIVE).build();
        em.persist(owner);
        restaurant = Restaurant.builder().owner(owner).name("Osh Markazi").slug("osh").build();
        em.persist(restaurant);
        consumer = User.builder().phone("998901234501")
                .role(Role.CONSUMER).status(UserStatus.ACTIVE).build();
        em.persist(consumer);
    }

    /** An order with a service fee, and the commission row that counts it. */
    private Order delivered(String subtotal, String serviceFee, LocalDateTime earnedAt) {
        Order order = Order.builder()
                .externalOrderNo("FD-" + System.nanoTime())
                .consumer(consumer).restaurant(restaurant)
                .orderType(OrderType.DELIVERY).status(OrderStatus.DELIVERED)
                .subtotal(new BigDecimal(subtotal))
                .serviceFee(new BigDecimal(serviceFee))
                .total(new BigDecimal(subtotal).add(new BigDecimal(serviceFee)))
                .build();
        em.persist(order);

        if (earnedAt != null) {
            em.persist(RestaurantCommission.builder()
                    .restaurantId(restaurant.getId()).orderId(order.getId())
                    .orderSubtotal(new BigDecimal(subtotal))
                    .commissionRate(new BigDecimal("15.00"))
                    .commissionAmount(new BigDecimal(subtotal).multiply(new BigDecimal("0.15")))
                    .earnedAt(earnedAt)
                    .build());
        }
        return order;
    }

    @Test
    @DisplayName("the fees of the period's orders are summed")
    void sumsThePeriod() {
        delivered("60000", "4800", LocalDateTime.of(2026, 9, 10, 12, 0));
        delivered("30000", "2400", LocalDateTime.of(2026, 9, 20, 12, 0));
        em.flush();

        assertThat(repository.getTotalServiceFee(PERIOD_START, PERIOD_END))
                .isEqualByComparingTo("7200");
    }

    @Test
    @DisplayName("orders outside the period are excluded")
    void excludesOtherPeriods() {
        delivered("60000", "4800", LocalDateTime.of(2026, 9, 10, 12, 0));
        delivered("99000", "7920", LocalDateTime.of(2026, 8, 15, 12, 0));
        delivered("99000", "7920", LocalDateTime.of(2026, 10, 2, 12, 0));
        em.flush();

        assertThat(repository.getTotalServiceFee(PERIOD_START, PERIOD_END))
                .isEqualByComparingTo("4800");
    }

    @Test
    @DisplayName("an order with no commission row is not counted")
    void countsTheSameOrdersAsCommission() {
        // THE property. Commission is recorded on delivery, so an order that
        // never got there is not in GMV or commission and must not be in
        // service-fee revenue either — otherwise the lines stop adding up and
        // no one can say which is wrong.
        delivered("60000", "4800", LocalDateTime.of(2026, 9, 10, 12, 0));
        delivered("50000", "4000", null);
        em.flush();

        assertThat(repository.getTotalServiceFee(PERIOD_START, PERIOD_END))
                .isEqualByComparingTo("4800");
    }

    @Test
    @DisplayName("it is counted on the same clock as commission, not the order's own")
    void usesTheCommissionClock() {
        // An order placed in August and delivered in September earns its
        // commission in September. Its fee belongs to the same period, or the
        // two lines drift apart at every month boundary.
        Order order = delivered("60000", "4800", LocalDateTime.of(2026, 9, 2, 12, 0));
        order.setCreatedAt(LocalDateTime.of(2026, 8, 31, 23, 0));
        em.flush();

        assertThat(repository.getTotalServiceFee(PERIOD_START, PERIOD_END))
                .isEqualByComparingTo("4800");
    }

    @Test
    @DisplayName("an empty period is zero, never null")
    void emptyPeriodIsZero() {
        // Summed straight into totalRevenue; a null here would be an NPE in a
        // report rather than a zero.
        assertThat(repository.getTotalServiceFee(PERIOD_START, PERIOD_END))
                .isNotNull()
                .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("a zero-rate order contributes nothing but is still counted as an order")
    void zeroFeeOrder() {
        // What the report looks like the day the rate is set to 0.
        delivered("60000", "0", LocalDateTime.of(2026, 9, 10, 12, 0));
        em.flush();

        assertThat(repository.getTotalServiceFee(PERIOD_START, PERIOD_END))
                .isEqualByComparingTo("0");
        assertThat(repository.getOrderCount(PERIOD_START, PERIOD_END)).isEqualTo(1);
    }
}
