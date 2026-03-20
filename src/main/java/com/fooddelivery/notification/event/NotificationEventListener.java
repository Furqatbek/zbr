package com.fooddelivery.notification.event;

import com.fooddelivery.notification.dto.OrderNotificationRequest;
import com.fooddelivery.notification.model.NotificationType;
import com.fooddelivery.notification.service.PersistentNotificationService;
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
 * Event listener for creating notifications based on domain events published via Spring's ApplicationEventPublisher.
 *
 * NOTE: Order-related events (OrderCreatedEvent, OrderStatusChangedEvent, CourierAssignedEvent, PaymentConfirmedEvent)
 * are processed by {@link com.fooddelivery.order.consumer.OrderEventConsumer} via RabbitMQ.
 * This listener handles events that are NOT published to RabbitMQ (support tickets, payouts, etc.).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final PersistentNotificationService notificationService;
    private final RestaurantRepository restaurantRepository;

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

                    if (restaurantUserId == null) {
                        log.warn("Restaurant {} has no owner assigned, skipping restaurant notification for payment failed event",
                                event.getRestaurantId());
                    }
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
