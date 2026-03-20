package com.fooddelivery.notification.event;

import com.fooddelivery.courier.entity.Courier;
import com.fooddelivery.courier.repository.CourierRepository;
import com.fooddelivery.notification.dto.OrderNotificationRequest;
import com.fooddelivery.notification.model.NotificationRole;
import com.fooddelivery.notification.model.NotificationType;
import com.fooddelivery.notification.service.PersistentNotificationService;
import com.fooddelivery.order.entity.OrderStatus;
import com.fooddelivery.order.event.CourierAssignedEvent;
import com.fooddelivery.order.event.OrderCreatedEvent;
import com.fooddelivery.order.event.OrderStatusChangedEvent;
import com.fooddelivery.order.event.PaymentConfirmedEvent;
import com.fooddelivery.restaurant.entity.Restaurant;
import com.fooddelivery.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Event listener for automatically creating notifications based on domain events.
 * Listens to order lifecycle events, payment events, and other business events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final PersistentNotificationService notificationService;
    private final RestaurantRepository restaurantRepository;
    private final CourierRepository courierRepository;

    /**
     * Handle order created event.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Handling OrderCreatedEvent for order: {}", event.getOrderId());

        try {
            // Fetch restaurant to get owner's user ID for notification targeting
            Long restaurantUserId = null;
            String restaurantName = null;
            if (event.getRestaurantId() != null) {
                Restaurant restaurant = restaurantRepository.findById(event.getRestaurantId()).orElse(null);
                if (restaurant != null) {
                    restaurantUserId = restaurant.getOwner() != null ? restaurant.getOwner().getId() : null;
                    restaurantName = restaurant.getName();
                }
            }

            OrderNotificationRequest request = OrderNotificationRequest.builder()
                    .orderId(event.getOrderId())
                    .orderNumber(event.getExternalOrderNo())
                    .customerId(event.getConsumerId())
                    .restaurantId(event.getRestaurantId())
                    .restaurantUserId(restaurantUserId)
                    .restaurantName(restaurantName)
                    .eventType(NotificationType.ORDER_CREATED)
                    .metadata(buildOrderCreatedMetadata(event))
                    .build();

            notificationService.notifyOrderCreated(request);
            log.debug("Order created notification sent for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to handle OrderCreatedEvent for order {}: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * Handle order status changed event.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Handling OrderStatusChangedEvent for order: {} ({} -> {})",
                event.getOrderId(), event.getPreviousStatus(), event.getNewStatus());

        try {
            OrderNotificationRequest request = buildStatusChangeRequest(event);

            switch (event.getNewStatus()) {
                case ACCEPTED -> notificationService.notifyOrderAccepted(request);
                case PREPARING -> notificationService.notifyOrderPreparing(request);
                case READY -> notificationService.notifyOrderReady(request);
                case COURIER_ASSIGNED -> notificationService.notifyCourierAssigned(request);
                case PICKED_UP -> notificationService.notifyOrderPickedUp(request);
                case IN_TRANSIT -> notificationService.notifyOrderInTransit(request);
                case DELIVERED -> notificationService.notifyOrderDelivered(request);
                case CANCELLED -> notificationService.notifyOrderCancelled(request);
                case REFUNDED -> notificationService.notifyRefundProcessed(request);
                default -> log.debug("No notification for status: {}", event.getNewStatus());
            }

            log.debug("Order status notification sent for order: {} ({})",
                    event.getOrderId(), event.getNewStatus());
        } catch (Exception e) {
            log.error("Failed to handle OrderStatusChangedEvent for order {}: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * Handle courier assigned event.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void handleCourierAssigned(CourierAssignedEvent event) {
        log.info("Handling CourierAssignedEvent for order: {}, courier: {}",
                event.getOrderId(), event.getCourierId());

        try {
            // Fetch courier to get user ID for notification targeting
            Long courierUserId = null;
            String courierName = null;
            if (event.getCourierId() != null) {
                Courier courier = courierRepository.findById(event.getCourierId()).orElse(null);
                if (courier != null && courier.getUser() != null) {
                    courierUserId = courier.getUser().getId();
                    courierName = courier.getUser().getFullName();
                }
            }

            // Fetch restaurant to get owner's user ID
            Long restaurantUserId = null;
            if (event.getRestaurantId() != null) {
                Restaurant restaurant = restaurantRepository.findById(event.getRestaurantId()).orElse(null);
                if (restaurant != null && restaurant.getOwner() != null) {
                    restaurantUserId = restaurant.getOwner().getId();
                }
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("courierId", event.getCourierId());
            metadata.put("pickupAddress", event.getPickupAddress());
            metadata.put("deliveryAddress", event.getDeliveryAddress());
            metadata.put("pickupLat", event.getPickupLat());
            metadata.put("pickupLng", event.getPickupLng());
            metadata.put("deliveryLat", event.getDeliveryLat());
            metadata.put("deliveryLng", event.getDeliveryLng());

            OrderNotificationRequest request = OrderNotificationRequest.builder()
                    .orderId(event.getOrderId())
                    .orderNumber(event.getExternalOrderNo())
                    .restaurantId(event.getRestaurantId())
                    .restaurantUserId(restaurantUserId)
                    .courierId(event.getCourierId())
                    .courierUserId(courierUserId)
                    .courierName(courierName)
                    .eventType(NotificationType.COURIER_ASSIGNED)
                    .metadata(metadata)
                    .build();

            notificationService.notifyCourierAssigned(request);
            log.debug("Courier assigned notification sent for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to handle CourierAssignedEvent for order {}: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * Handle payment confirmed event.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void handlePaymentConfirmed(PaymentConfirmedEvent event) {
        log.info("Handling PaymentConfirmedEvent for order: {}, amount: {}",
                event.getOrderId(), event.getAmount());

        try {
            // Fetch restaurant to get owner's user ID for notification targeting
            Long restaurantUserId = null;
            String restaurantName = null;
            if (event.getRestaurantId() != null) {
                Restaurant restaurant = restaurantRepository.findById(event.getRestaurantId()).orElse(null);
                if (restaurant != null) {
                    restaurantUserId = restaurant.getOwner() != null ? restaurant.getOwner().getId() : null;
                    restaurantName = restaurant.getName();
                }
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("paymentId", event.getPaymentId());
            metadata.put("amount", event.getAmount());
            metadata.put("provider", event.getProvider());
            metadata.put("providerPaymentId", event.getProviderPaymentId());

            OrderNotificationRequest request = OrderNotificationRequest.builder()
                    .orderId(event.getOrderId())
                    .orderNumber(event.getExternalOrderNo())
                    .customerId(event.getConsumerId())
                    .restaurantId(event.getRestaurantId())
                    .restaurantUserId(restaurantUserId)
                    .restaurantName(restaurantName)
                    .eventType(NotificationType.PAYMENT_RECEIVED)
                    .totalAmount(event.getAmount())
                    .metadata(metadata)
                    .build();

            notificationService.notifyPaymentReceived(request);
            log.debug("Payment confirmed notification sent for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to handle PaymentConfirmedEvent for order {}: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * Build order notification request from status change event.
     */
    private OrderNotificationRequest buildStatusChangeRequest(OrderStatusChangedEvent event) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("previousStatus", event.getPreviousStatus().name());
        metadata.put("newStatus", event.getNewStatus().name());

        if (event.getReason() != null) {
            metadata.put("reason", event.getReason());
        }

        // Fetch restaurant to get owner's user ID for notification targeting
        Long restaurantUserId = null;
        String restaurantName = null;
        if (event.getRestaurantId() != null) {
            Restaurant restaurant = restaurantRepository.findById(event.getRestaurantId()).orElse(null);
            if (restaurant != null) {
                restaurantUserId = restaurant.getOwner() != null ? restaurant.getOwner().getId() : null;
                restaurantName = restaurant.getName();
            }
        }

        // Fetch courier to get user ID for notification targeting
        Long courierUserId = null;
        String courierName = null;
        if (event.getCourierId() != null) {
            Courier courier = courierRepository.findById(event.getCourierId()).orElse(null);
            if (courier != null && courier.getUser() != null) {
                courierUserId = courier.getUser().getId();
                courierName = courier.getUser().getFullName();
            }
        }

        return OrderNotificationRequest.builder()
                .orderId(event.getOrderId())
                .orderNumber(event.getExternalOrderNo())
                .customerId(event.getConsumerId())
                .restaurantId(event.getRestaurantId())
                .restaurantUserId(restaurantUserId)
                .restaurantName(restaurantName)
                .courierId(event.getCourierId())
                .courierUserId(courierUserId)
                .courierName(courierName)
                .eventType(mapStatusToNotificationType(event.getNewStatus()))
                .cancellationReason(event.getReason())
                .metadata(metadata)
                .build();
    }

    /**
     * Build metadata for order created event.
     */
    private Map<String, Object> buildOrderCreatedMetadata(OrderCreatedEvent event) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderNumber", event.getExternalOrderNo());
        metadata.put("consumerId", event.getConsumerId());
        metadata.put("restaurantId", event.getRestaurantId());
        return metadata;
    }

    /**
     * Map order status to notification type.
     */
    private NotificationType mapStatusToNotificationType(OrderStatus status) {
        return switch (status) {
            case CREATED -> NotificationType.ORDER_CREATED;
            case ACCEPTED -> NotificationType.ORDER_ACCEPTED;
            case PREPARING -> NotificationType.ORDER_PREPARING;
            case READY -> NotificationType.ORDER_READY;
            case COURIER_ASSIGNED -> NotificationType.COURIER_ASSIGNED;
            case PICKED_UP -> NotificationType.ORDER_PICKED_UP;
            case IN_TRANSIT -> NotificationType.ORDER_IN_TRANSIT;
            case DELIVERED -> NotificationType.ORDER_DELIVERED;
            case COMPLETED -> NotificationType.ORDER_COMPLETED;
            case CANCELLED -> NotificationType.ORDER_CANCELLED;
            case REFUNDED -> NotificationType.REFUND_PROCESSED;
        };
    }

    // ===== Support Ticket Events =====

    /**
     * Handle support ticket event.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void handleSupportTicketEvent(SupportTicketEvent event) {
        log.info("Handling SupportTicketEvent for ticket: {}, type: {}",
                event.getTicketId(), event.getTicketEventType());

        try {
            switch (event.getTicketEventType()) {
                case CREATED -> notificationService.notifySupportTicketCreated(
                        event.getUserId(), event.getTicketId(), event.getSubject());
                case UPDATED -> notificationService.notifySupportTicketUpdated(
                        event.getUserId(), event.getTicketId(), event.getMessage());
                case RESOLVED -> notificationService.notifySupportTicketResolved(
                        event.getUserId(), event.getTicketId());
                default -> log.debug("No notification for ticket event type: {}",
                        event.getTicketEventType());
            }

            log.debug("Support ticket notification sent for ticket: {}", event.getTicketId());
        } catch (Exception e) {
            log.error("Failed to handle SupportTicketEvent for ticket {}: {}",
                    event.getTicketId(), e.getMessage(), e);
        }
    }

    // ===== Payment Events =====

    /**
     * Handle payment failed event.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Handling PaymentFailedEvent for order: {}, reason: {}",
                event.getOrderId(), event.getFailureReason());

        try {
            // Fetch restaurant to get owner's user ID for notification targeting
            Long restaurantUserId = null;
            String restaurantName = null;
            if (event.getRestaurantId() != null) {
                Restaurant restaurant = restaurantRepository.findById(event.getRestaurantId()).orElse(null);
                if (restaurant != null) {
                    restaurantUserId = restaurant.getOwner() != null ? restaurant.getOwner().getId() : null;
                    restaurantName = restaurant.getName();
                }
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("paymentId", event.getPaymentId());
            metadata.put("amount", event.getAmount());
            metadata.put("failureReason", event.getFailureReason());
            metadata.put("failureCode", event.getFailureCode());
            metadata.put("provider", event.getProvider());

            OrderNotificationRequest request = OrderNotificationRequest.builder()
                    .orderId(event.getOrderId())
                    .orderNumber(event.getExternalOrderNo())
                    .customerId(event.getConsumerId())
                    .restaurantId(event.getRestaurantId())
                    .restaurantUserId(restaurantUserId)
                    .restaurantName(restaurantName)
                    .eventType(NotificationType.PAYMENT_FAILED)
                    .totalAmount(event.getAmount())
                    .cancellationReason(event.getFailureReason())
                    .metadata(metadata)
                    .build();

            notificationService.notifyPaymentFailed(request);
            log.debug("Payment failed notification sent for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to handle PaymentFailedEvent for order {}: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }

    // ===== Payout Events =====

    /**
     * Handle payout issued event.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    public void handlePayoutIssued(PayoutIssuedEvent event) {
        log.info("Handling PayoutIssuedEvent for recipient: {}, amount: {}",
                event.getRecipientId(), event.getAmount());

        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("payoutId", event.getPayoutId());
            metadata.put("currency", event.getCurrency());
            metadata.put("payoutMethod", event.getPayoutMethod());
            metadata.put("periodStart", event.getPeriodStart());
            metadata.put("periodEnd", event.getPeriodEnd());

            if (event.getMetadata() != null) {
                metadata.putAll(event.getMetadata());
            }

            String formattedAmount = String.format("%s %s",
                    event.getCurrency() != null ? event.getCurrency() : "USD",
                    event.getAmount().toPlainString());

            notificationService.notifyPayoutIssued(
                    event.getRecipientId(),
                    event.getRecipientRole(),
                    formattedAmount,
                    metadata);

            log.debug("Payout notification sent for recipient: {}", event.getRecipientId());
        } catch (Exception e) {
            log.error("Failed to handle PayoutIssuedEvent for recipient {}: {}",
                    event.getRecipientId(), e.getMessage(), e);
        }
    }
}
