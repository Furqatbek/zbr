package com.fooddelivery.payment.consumer;

import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.notification.dto.OrderNotificationRequest;
import com.fooddelivery.notification.event.PaymentFailedEvent;
import com.fooddelivery.notification.model.NotificationType;
import com.fooddelivery.notification.service.PersistentNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumer for payment-related events from RabbitMQ.
 * Processes payment failure events and triggers appropriate notifications.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final PersistentNotificationService notificationService;

    /**
     * Process payment failed events from RabbitMQ.
     */
    @RabbitListener(
            queues = RabbitMQConfig.PAYMENT_FAILED_QUEUE,
            id = "paymentFailedListener",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Consuming PaymentFailedEvent from queue: orderId={}, reason={}",
                event.getOrderId(), event.getFailureReason());

        try {
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
                    .eventType(NotificationType.PAYMENT_FAILED)
                    .totalAmount(event.getAmount())
                    .cancellationReason(event.getFailureReason())
                    .metadata(metadata)
                    .build();

            notificationService.notifyPaymentFailed(request);
            log.debug("Payment failed notification processed for order: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process PaymentFailedEvent for order {}: {}",
                    event.getOrderId(), e.getMessage(), e);
            throw e; // Re-throw to trigger DLQ handling
        }
    }
}
