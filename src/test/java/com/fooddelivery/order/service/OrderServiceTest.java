package com.fooddelivery.order.service;

import com.fooddelivery.analytics.financial.service.CommissionService;
import com.fooddelivery.auth.entity.User;
import com.fooddelivery.auth.service.UserService;
import com.fooddelivery.common.event.EventPublisher;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.InvalidOperationException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.courier.repository.CourierRepository;
import com.fooddelivery.order.dto.CancelOrderRequest;
import com.fooddelivery.order.dto.CreateOrderRequest;
import com.fooddelivery.order.dto.OrderDto;
import com.fooddelivery.order.dto.UpdateOrderStatusRequest;
import com.fooddelivery.order.entity.Order;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.entity.OrderType;
import com.fooddelivery.order.entity.PaymentStatus;
import com.fooddelivery.order.mapper.OrderMapper;
import com.fooddelivery.order.repository.OrderRepository;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.repository.MenuItemRepository;
import com.fooddelivery.restaurant.service.RestaurantService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Orchestration tests for {@link OrderService}, focused on the money path:
 * order-status transition guarding and refund-on-cancel. Entities are mocked so
 * the tests exercise the service's decisions (does it guard the transition, does
 * it refund a paid order, does it persist) without depending on entity internals
 * — the real state machine itself is covered by OrderStatusTest.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderService money-path tests")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private MenuItemRepository menuItemRepository;
    @Mock private RestaurantService restaurantService;
    @Mock private UserService userService;
    @Mock private OrderMapper orderMapper;
    @Mock private EventPublisher eventPublisher;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private CourierRepository courierRepository;
    @Mock private DeliveryFeeCalculationService deliveryFeeCalculationService;
    @Mock private CommissionService commissionService;
    @Mock private PaymentService paymentService;

    @InjectMocks
    private OrderService orderService;

    /** A mock Order wired with the getters the service dereferences unconditionally. */
    private Order mockOrder(OrderStatus status, PaymentStatus paymentStatus) {
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(1L);
        when(order.getExternalOrderNo()).thenReturn("ORD-TEST-001");
        when(order.getStatus()).thenReturn(status);
        when(order.getPaymentStatus()).thenReturn(paymentStatus);
        Restaurant restaurant = mock(Restaurant.class);
        when(restaurant.getId()).thenReturn(10L);
        User consumer = mock(User.class);
        when(consumer.getId()).thenReturn(20L);
        when(order.getRestaurant()).thenReturn(restaurant);
        when(order.getConsumer()).thenReturn(consumer);
        when(order.getCourier()).thenReturn(null);
        return order;
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("rejects ordering from a closed restaurant")
        void rejectsClosedRestaurant() {
            Restaurant restaurant = mock(Restaurant.class);
            when(restaurant.isCurrentlyOpen()).thenReturn(false);
            when(restaurantService.getRestaurantEntityById(1L)).thenReturn(restaurant);

            CreateOrderRequest request = CreateOrderRequest.builder()
                    .restaurantId(1L)
                    .orderType(OrderType.DINE_IN)
                    .build();

            assertThatThrownBy(() -> orderService.createOrder(1L, request, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("closed");
        }

        @Test
        @DisplayName("idempotent replay returns the existing order without creating a new one")
        void idempotentReplay() {
            Order existing = mock(Order.class);
            when(orderRepository.findByIdempotencyKey("key-123")).thenReturn(Optional.of(existing));
            OrderDto dto = mock(OrderDto.class);
            when(orderMapper.toDto(existing)).thenReturn(dto);

            OrderDto result = orderService.createOrder(20L, CreateOrderRequest.builder().build(), "key-123");

            assertThat(result).isSameAs(dto);
            // The key short-circuits before any order work happens.
            verify(orderRepository, never()).save(any());
            verifyNoInteractions(restaurantService, userService, deliveryFeeCalculationService);
        }
    }

    @Nested
    @DisplayName("updateOrderStatus")
    class UpdateOrderStatus {

        @Test
        @DisplayName("advances the order when the transition is valid")
        void validTransition() {
            Order order = mockOrder(OrderStatus.CREATED, PaymentStatus.PENDING);
            when(order.canTransitionTo(OrderStatus.ACCEPTED)).thenReturn(true);
            when(orderRepository.findByIdWithLock(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            OrderDto dto = mock(OrderDto.class);
            when(orderMapper.toDto(order)).thenReturn(dto);

            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.ACCEPTED)
                    .build();

            OrderDto result = orderService.updateOrderStatus(1L, request);

            assertThat(result).isSameAs(dto);
            verify(order).updateStatus(OrderStatus.ACCEPTED);
            verify(orderRepository).save(order);
            verify(eventPublisher).publishAsync(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("rejects an invalid transition")
        void invalidTransition() {
            Order order = mockOrder(OrderStatus.DELIVERED, PaymentStatus.CONFIRMED);
            when(order.canTransitionTo(OrderStatus.PREPARING)).thenReturn(false);
            when(orderRepository.findByIdWithLock(1L)).thenReturn(Optional.of(order));

            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.PREPARING)
                    .build();

            assertThatThrownBy(() -> orderService.updateOrderStatus(1L, request))
                    .isInstanceOf(InvalidOperationException.class);
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when the order does not exist")
        void orderNotFound() {
            when(orderRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

            UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                    .status(OrderStatus.ACCEPTED)
                    .build();

            assertThatThrownBy(() -> orderService.updateOrderStatus(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        @Test
        @DisplayName("cancels a cancellable, unpaid order without issuing a refund")
        void cancelsUnpaidOrder() {
            Order order = mockOrder(OrderStatus.CREATED, PaymentStatus.PENDING);
            when(order.isCancellable()).thenReturn(true);
            when(orderRepository.findByIdWithLock(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            OrderDto dto = mock(OrderDto.class);
            when(orderMapper.toDto(order)).thenReturn(dto);

            CancelOrderRequest request = CancelOrderRequest.builder()
                    .reason("Customer changed mind")
                    .build();

            OrderDto result = orderService.cancelOrder(1L, request, 20L);

            assertThat(result).isSameAs(dto);
            verify(order).updateStatus(OrderStatus.CANCELLED);
            verify(order).setCancellationReason("Customer changed mind");
            verify(orderRepository).save(order);
            // Unpaid order: money was never taken, so no refund should be attempted.
            verify(paymentService, never()).refundPayment(anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("refunds a cancellable order whose payment was confirmed")
        void refundsPaidOrder() {
            Order order = mockOrder(OrderStatus.CREATED, PaymentStatus.CONFIRMED);
            when(order.isCancellable()).thenReturn(true);
            when(orderRepository.findByIdWithLock(1L)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);
            when(orderMapper.toDto(order)).thenReturn(mock(OrderDto.class));

            CancelOrderRequest request = CancelOrderRequest.builder()
                    .reason("Restaurant unavailable")
                    .build();

            orderService.cancelOrder(1L, request, 20L);

            // Paid order: the customer's money must be refunded on cancellation.
            verify(paymentService).refundPayment(eq(1L), isNull(), eq("Restaurant unavailable"));
        }

        @Test
        @DisplayName("refuses to cancel an order that is no longer cancellable")
        void refusesNonCancellable() {
            Order order = mockOrder(OrderStatus.DELIVERED, PaymentStatus.CONFIRMED);
            when(order.isCancellable()).thenReturn(false);
            when(orderRepository.findByIdWithLock(1L)).thenReturn(Optional.of(order));

            CancelOrderRequest request = CancelOrderRequest.builder()
                    .reason("Too late")
                    .build();

            assertThatThrownBy(() -> orderService.cancelOrder(1L, request, 20L))
                    .isInstanceOf(InvalidOperationException.class);
            verify(orderRepository, never()).save(any());
            verify(paymentService, never()).refundPayment(anyLong(), any(), anyString());
        }
    }
}
