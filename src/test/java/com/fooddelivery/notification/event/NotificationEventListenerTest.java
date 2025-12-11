package com.fooddelivery.notification.event;

import com.fooddelivery.notification.dto.OrderNotificationRequest;
import com.fooddelivery.notification.model.NotificationRole;
import com.fooddelivery.notification.model.NotificationType;
import com.fooddelivery.notification.service.PersistentNotificationService;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.event.CourierAssignedEvent;
import com.fooddelivery.order.event.OrderCreatedEvent;
import com.fooddelivery.order.event.OrderStatusChangedEvent;
import com.fooddelivery.order.event.PaymentConfirmedEvent;
import com.fooddelivery.order.entity.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener Unit Tests")
class NotificationEventListenerTest {

    @Mock
    private PersistentNotificationService notificationService;

    @InjectMocks
    private NotificationEventListener eventListener;

    @Nested
    @DisplayName("handleOrderCreated Tests")
    class HandleOrderCreatedTests {

        @Test
        @DisplayName("Should handle order created event")
        void shouldHandleOrderCreatedEvent() {
            // Arrange
            OrderCreatedEvent event = new OrderCreatedEvent(
                    100L, "ORD-100", 1L, 10L, OrderType.DELIVERY,
                    BigDecimal.valueOf(50.00), "123 Test St");

            // Act
            eventListener.handleOrderCreated(event);

            // Assert
            ArgumentCaptor<OrderNotificationRequest> captor = ArgumentCaptor.forClass(OrderNotificationRequest.class);
            verify(notificationService).notifyOrderCreated(captor.capture());

            OrderNotificationRequest request = captor.getValue();
            assertThat(request.getOrderId()).isEqualTo(100L);
            assertThat(request.getOrderNumber()).isEqualTo("ORD-100");
            assertThat(request.getCustomerId()).isEqualTo(1L);
            assertThat(request.getRestaurantId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Should handle exception gracefully")
        void shouldHandleExceptionGracefully() {
            // Arrange
            OrderCreatedEvent event = new OrderCreatedEvent(
                    100L, "ORD-100", 1L, 10L, OrderType.DELIVERY,
                    BigDecimal.valueOf(50.00), "123 Test St");

            doThrow(new RuntimeException("Service error"))
                    .when(notificationService).notifyOrderCreated(any());

            // Act - should not throw
            eventListener.handleOrderCreated(event);

            // Assert
            verify(notificationService).notifyOrderCreated(any());
        }
    }

    @Nested
    @DisplayName("handleOrderStatusChanged Tests")
    class HandleOrderStatusChangedTests {

        @Test
        @DisplayName("Should notify when order accepted")
        void shouldNotifyWhenOrderAccepted() {
            // Arrange
            OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                    100L, "ORD-100", 10L, 1L, null,
                    OrderStatus.CREATED, OrderStatus.ACCEPTED, null);

            // Act
            eventListener.handleOrderStatusChanged(event);

            // Assert
            verify(notificationService).notifyOrderAccepted(any(OrderNotificationRequest.class));
        }

        @Test
        @DisplayName("Should notify when order preparing")
        void shouldNotifyWhenOrderPreparing() {
            // Arrange
            OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                    100L, "ORD-100", 10L, 1L, null,
                    OrderStatus.ACCEPTED, OrderStatus.PREPARING, null);

            // Act
            eventListener.handleOrderStatusChanged(event);

            // Assert
            verify(notificationService).notifyOrderPreparing(any(OrderNotificationRequest.class));
        }

        @Test
        @DisplayName("Should notify when order ready")
        void shouldNotifyWhenOrderReady() {
            // Arrange
            OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                    100L, "ORD-100", 10L, 1L, 20L,
                    OrderStatus.PREPARING, OrderStatus.READY, null);

            // Act
            eventListener.handleOrderStatusChanged(event);

            // Assert
            verify(notificationService).notifyOrderReady(any(OrderNotificationRequest.class));
        }

        @Test
        @DisplayName("Should notify when order picked up")
        void shouldNotifyWhenOrderPickedUp() {
            // Arrange
            OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                    100L, "ORD-100", 10L, 1L, 20L,
                    OrderStatus.READY, OrderStatus.PICKED_UP, null);

            // Act
            eventListener.handleOrderStatusChanged(event);

            // Assert
            verify(notificationService).notifyOrderPickedUp(any(OrderNotificationRequest.class));
        }

        @Test
        @DisplayName("Should notify when order in transit")
        void shouldNotifyWhenOrderInTransit() {
            // Arrange
            OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                    100L, "ORD-100", 10L, 1L, 20L,
                    OrderStatus.PICKED_UP, OrderStatus.IN_TRANSIT, null);

            // Act
            eventListener.handleOrderStatusChanged(event);

            // Assert
            verify(notificationService).notifyOrderInTransit(any(OrderNotificationRequest.class));
        }

        @Test
        @DisplayName("Should notify when order delivered")
        void shouldNotifyWhenOrderDelivered() {
            // Arrange
            OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                    100L, "ORD-100", 10L, 1L, 20L,
                    OrderStatus.IN_TRANSIT, OrderStatus.DELIVERED, null);

            // Act
            eventListener.handleOrderStatusChanged(event);

            // Assert
            verify(notificationService).notifyOrderDelivered(any(OrderNotificationRequest.class));
        }

        @Test
        @DisplayName("Should notify when order cancelled with reason")
        void shouldNotifyWhenOrderCancelledWithReason() {
            // Arrange
            OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                    100L, "ORD-100", 10L, 1L, null,
                    OrderStatus.ACCEPTED, OrderStatus.CANCELLED, "Customer requested cancellation");

            // Act
            eventListener.handleOrderStatusChanged(event);

            // Assert
            ArgumentCaptor<OrderNotificationRequest> captor = ArgumentCaptor.forClass(OrderNotificationRequest.class);
            verify(notificationService).notifyOrderCancelled(captor.capture());

            OrderNotificationRequest request = captor.getValue();
            assertThat(request.getCancellationReason()).isEqualTo("Customer requested cancellation");
        }

        @Test
        @DisplayName("Should notify when order refunded")
        void shouldNotifyWhenOrderRefunded() {
            // Arrange
            OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                    100L, "ORD-100", 10L, 1L, null,
                    OrderStatus.CANCELLED, OrderStatus.REFUNDED, null);

            // Act
            eventListener.handleOrderStatusChanged(event);

            // Assert
            verify(notificationService).notifyRefundProcessed(any(OrderNotificationRequest.class));
        }
    }

    @Nested
    @DisplayName("handleCourierAssigned Tests")
    class HandleCourierAssignedTests {

        @Test
        @DisplayName("Should handle courier assigned event")
        void shouldHandleCourierAssignedEvent() {
            // Arrange
            CourierAssignedEvent event = new CourierAssignedEvent(
                    100L, "ORD-100", 20L, 10L,
                    "456 Restaurant St", "123 Customer St",
                    BigDecimal.valueOf(40.7128), BigDecimal.valueOf(-74.0060),
                    BigDecimal.valueOf(40.7589), BigDecimal.valueOf(-73.9851));

            // Act
            eventListener.handleCourierAssigned(event);

            // Assert
            ArgumentCaptor<OrderNotificationRequest> captor = ArgumentCaptor.forClass(OrderNotificationRequest.class);
            verify(notificationService).notifyCourierAssigned(captor.capture());

            OrderNotificationRequest request = captor.getValue();
            assertThat(request.getOrderId()).isEqualTo(100L);
            assertThat(request.getCourierId()).isEqualTo(20L);
            assertThat(request.getMetadata()).containsKey("pickupAddress");
            assertThat(request.getMetadata()).containsKey("deliveryAddress");
        }
    }

    @Nested
    @DisplayName("handlePaymentConfirmed Tests")
    class HandlePaymentConfirmedTests {

        @Test
        @DisplayName("Should handle payment confirmed event")
        void shouldHandlePaymentConfirmedEvent() {
            // Arrange
            PaymentConfirmedEvent event = new PaymentConfirmedEvent(
                    500L, 100L, "ORD-100", 1L, 10L,
                    BigDecimal.valueOf(55.99), "STRIPE", "pi_123456");

            // Act
            eventListener.handlePaymentConfirmed(event);

            // Assert
            ArgumentCaptor<OrderNotificationRequest> captor = ArgumentCaptor.forClass(OrderNotificationRequest.class);
            verify(notificationService).notifyPaymentReceived(captor.capture());

            OrderNotificationRequest request = captor.getValue();
            assertThat(request.getOrderId()).isEqualTo(100L);
            assertThat(request.getTotalAmount()).isEqualTo(BigDecimal.valueOf(55.99));
            assertThat(request.getMetadata()).containsKey("paymentId");
            assertThat(request.getMetadata()).containsKey("provider");
        }
    }

    @Nested
    @DisplayName("handleSupportTicketEvent Tests")
    class HandleSupportTicketEventTests {

        @Test
        @DisplayName("Should handle support ticket created event")
        void shouldHandleSupportTicketCreatedEvent() {
            // Arrange
            SupportTicketEvent event = new SupportTicketEvent(
                    200L, 1L, "Issue with order",
                    SupportTicketEvent.TicketEventType.CREATED, null);

            // Act
            eventListener.handleSupportTicketEvent(event);

            // Assert
            verify(notificationService).notifySupportTicketCreated(1L, 200L, "Issue with order");
        }

        @Test
        @DisplayName("Should handle support ticket updated event")
        void shouldHandleSupportTicketUpdatedEvent() {
            // Arrange
            SupportTicketEvent event = new SupportTicketEvent(
                    200L, 1L, "Issue with order",
                    SupportTicketEvent.TicketEventType.UPDATED, "Your ticket has been assigned");

            // Act
            eventListener.handleSupportTicketEvent(event);

            // Assert
            verify(notificationService).notifySupportTicketUpdated(1L, 200L, "Your ticket has been assigned");
        }

        @Test
        @DisplayName("Should handle support ticket resolved event")
        void shouldHandleSupportTicketResolvedEvent() {
            // Arrange
            SupportTicketEvent event = new SupportTicketEvent(
                    200L, 1L, "Issue with order",
                    SupportTicketEvent.TicketEventType.RESOLVED, null);

            // Act
            eventListener.handleSupportTicketEvent(event);

            // Assert
            verify(notificationService).notifySupportTicketResolved(1L, 200L);
        }
    }

    @Nested
    @DisplayName("handlePaymentFailed Tests")
    class HandlePaymentFailedTests {

        @Test
        @DisplayName("Should handle payment failed event")
        void shouldHandlePaymentFailedEvent() {
            // Arrange
            PaymentFailedEvent event = new PaymentFailedEvent(
                    500L, 100L, "ORD-100", 1L, 10L,
                    BigDecimal.valueOf(55.99), "Insufficient funds",
                    "insufficient_funds", "STRIPE");

            // Act
            eventListener.handlePaymentFailed(event);

            // Assert
            ArgumentCaptor<OrderNotificationRequest> captor = ArgumentCaptor.forClass(OrderNotificationRequest.class);
            verify(notificationService).notifyPaymentFailed(captor.capture());

            OrderNotificationRequest request = captor.getValue();
            assertThat(request.getOrderId()).isEqualTo(100L);
            assertThat(request.getCancellationReason()).isEqualTo("Insufficient funds");
            assertThat(request.getMetadata()).containsKey("failureCode");
        }
    }

    @Nested
    @DisplayName("handlePayoutIssued Tests")
    class HandlePayoutIssuedTests {

        @Test
        @DisplayName("Should handle payout issued event for restaurant")
        void shouldHandlePayoutIssuedEventForRestaurant() {
            // Arrange
            PayoutIssuedEvent event = new PayoutIssuedEvent(
                    300L, 10L, NotificationRole.RESTAURANT,
                    BigDecimal.valueOf(1500.00), "USD", "BANK_TRANSFER",
                    "2024-01-01", "2024-01-15", Map.of("orderCount", 50));

            // Act
            eventListener.handlePayoutIssued(event);

            // Assert
            verify(notificationService).notifyPayoutIssued(
                    eq(10L),
                    eq(NotificationRole.RESTAURANT),
                    eq("USD 1500.00"),
                    any(Map.class));
        }

        @Test
        @DisplayName("Should handle payout issued event for courier")
        void shouldHandlePayoutIssuedEventForCourier() {
            // Arrange
            PayoutIssuedEvent event = new PayoutIssuedEvent(
                    301L, 20L, NotificationRole.COURIER,
                    BigDecimal.valueOf(350.50), "USD", "INSTANT_PAYOUT",
                    "2024-01-08", "2024-01-15", null);

            // Act
            eventListener.handlePayoutIssued(event);

            // Assert
            verify(notificationService).notifyPayoutIssued(
                    eq(20L),
                    eq(NotificationRole.COURIER),
                    eq("USD 350.50"),
                    any(Map.class));
        }
    }
}
