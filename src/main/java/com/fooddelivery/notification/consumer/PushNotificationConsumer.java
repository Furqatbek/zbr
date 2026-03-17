package com.fooddelivery.notification.consumer;

import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.notification.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumer for push notification requests from RabbitMQ.
 * Processes push notifications and sends them via configured push provider.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.push", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PushNotificationConsumer {

    /**
     * Process push notification requests from the queue.
     */
    @RabbitListener(
            queues = RabbitMQConfig.NOTIFICATION_PUSH_QUEUE,
            id = "pushNotificationListener",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handlePushNotification(NotificationRequest request) {
        log.info("Consuming push notification from queue: userId={}, subject={}",
                request.getUserId(), request.getSubject());

        if (request.getDeviceToken() == null || request.getDeviceToken().isBlank()) {
            log.warn("Push notification skipped: no device token provided for user {}",
                    request.getUserId());
            return;
        }

        if (request.getBody() == null || request.getBody().isBlank()) {
            log.warn("Push notification skipped: no message body provided");
            return;
        }

        try {
            // TODO: Integrate with actual push provider (FCM, APNS, etc.)
            sendPushNotification(request);
            log.info("Push notification sent successfully to user: {}", request.getUserId());

        } catch (Exception e) {
            log.error("Failed to send push notification to user {}: {}",
                    request.getUserId(), e.getMessage(), e);
            throw e; // Re-throw to trigger DLQ handling
        }
    }

    /**
     * Send push notification via configured provider.
     * Placeholder for actual push provider integration.
     */
    private void sendPushNotification(NotificationRequest request) {
        // Log push details for now
        log.info("Sending push notification:\n  UserId: {}\n  Token: {}...\n  Title: {}\n  Body: {}...",
                request.getUserId(),
                request.getDeviceToken().length() > 20 ?
                        request.getDeviceToken().substring(0, 20) : request.getDeviceToken(),
                request.getSubject(),
                request.getBody().length() > 100 ?
                        request.getBody().substring(0, 100) : request.getBody());

        // TODO: Implement actual push notification sending
        // Example integration points:
        // - Firebase Cloud Messaging (FCM)
        // - Apple Push Notification Service (APNS)
        // - OneSignal
        // - Pusher
    }
}
