package com.fooddelivery.courier.service;

import com.fooddelivery.analytics.financial.service.CommissionService;
import com.fooddelivery.auth.service.UserService;
import com.fooddelivery.common.event.EventPublisher;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.entity.CourierStatus;
import com.fooddelivery.courier.repository.CourierRepository;
import com.fooddelivery.notification.service.PersistentNotificationService;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.repository.DeliveryIssueRepository;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.order.repository.PaymentRepository;
import com.fooddelivery.restaurant.entity.Restaurant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Picking an order up from the restaurant.
 *
 * <p>Order status is ONE field written by two actors. The kitchen moves it
 * ACCEPTED → PREPARING → READY; the courier moves it COURIER_ASSIGNED →
 * PICKED_UP → IN_TRANSIT. Couriers are dispatched while the food is still
 * cooking, so the ordinary sequence is: courier assigned, THEN restaurant marks
 * ready — at which point READY overwrites COURIER_ASSIGNED.
 *
 * <p>The pickup guard did not accept READY, so a courier standing in the shop
 * with the food in front of them was told the order "cannot be picked up in
 * current status: READY". getActiveOrders already listed READY as an active
 * courier state, so only this one guard disagreed with the rest of the system.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CourierService.confirmPickup")
class CourierPickupTest {

    private static final Long COURIER_ID = 9L;
    private static final Long ORDER_ID = 77L;

    @Mock private CourierRepository courierRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private DeliveryIssueRepository deliveryIssueRepository;
    @Mock private UserService userService;
    @Mock private EventPublisher eventPublisher;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private PersistentNotificationService notificationService;
    @Mock private CommissionService commissionService;

    @InjectMocks
    private CourierService courierService;

    private Order order(OrderStatus status, LocalDateTime readyAt) {
        Courier courier = Courier.builder()
                .id(COURIER_ID).status(CourierStatus.AVAILABLE).verified(true).build();

        Order order = Order.builder()
                .id(ORDER_ID)
                .restaurant(Restaurant.builder().id(3L).name("Osh Markazi")
                        .phone("998712001122").build())
                .courier(courier)
                .consumer(com.fooddelivery.auth.entity.User.builder()
                        .id(42L).phone("998901234567").build())
                .status(status)
                .readyAt(readyAt)
                .deliveryFee(new BigDecimal("15000"))
                .build();

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        return order;
    }

    @Test
    @DisplayName("READY is accepted — the restaurant finished cooking after the courier was assigned")
    void picksUpFromReady() {
        // THE regression. This is the ordinary sequence, and it was rejected.
        Order order = order(OrderStatus.READY, LocalDateTime.now());

        courierService.confirmPickup(COURIER_ID, ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PICKED_UP);
    }

    @Test
    @DisplayName("COURIER_ASSIGNED is accepted — the food was ready before the courier arrived")
    void picksUpFromCourierAssigned() {
        Order order = order(OrderStatus.COURIER_ASSIGNED, LocalDateTime.now());

        courierService.confirmPickup(COURIER_ID, ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PICKED_UP);
    }

    @Test
    @DisplayName("the food still has to be ready — readyAt, not the status label, decides")
    void refusesBeforeTheKitchenIsDone() {
        order(OrderStatus.COURIER_ASSIGNED, null);

        assertThatThrownBy(() -> courierService.confirmPickup(COURIER_ID, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not ready for pickup yet");
    }

    @Test
    @DisplayName("a repeated pickup is a no-op, not an error")
    void isIdempotent() {
        // A flaky response must not leave the courier unable to proceed.
        Order order = order(OrderStatus.PICKED_UP, LocalDateTime.now());

        courierService.confirmPickup(COURIER_ID, ORDER_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PICKED_UP);
    }

    @Test
    @DisplayName("a status that is genuinely wrong is still refused")
    void refusesDeliveredOrder() {
        order(OrderStatus.DELIVERED, LocalDateTime.now());

        assertThatThrownBy(() -> courierService.confirmPickup(COURIER_ID, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be picked up in current status");
    }

    @Test
    @DisplayName("another courier's order is refused before any status check")
    void refusesSomeoneElsesOrder() {
        order(OrderStatus.READY, LocalDateTime.now());

        assertThatThrownBy(() -> courierService.confirmPickup(999L, ORDER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not assigned to you");
    }
}
