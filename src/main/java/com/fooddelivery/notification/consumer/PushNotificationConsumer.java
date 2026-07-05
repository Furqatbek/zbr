package com.fooddelivery.notification.consumer;

import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.notification.dto.NotificationRequest;
import com.fooddelivery.notification.entity.UserDeviceToken;
import com.fooddelivery.notification.repository.UserDeviceTokenRepository;
import com.fooddelivery.notification.service.ExpoPushService;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Consumer for push notification requests from RabbitMQ.
 * Processes push notifications and sends them via Firebase Cloud Messaging.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.push", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PushNotificationConsumer {

    private final UserDeviceTokenRepository deviceTokenRepository;
    private final ExpoPushService expoPushService;

    @Autowired(required = false)
    private FirebaseMessaging firebaseMessaging;

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

        if (request.getUserId() == null) {
            log.warn("Push notification skipped: no userId provided");
            return;
        }

        if (request.getBody() == null || request.getBody().isBlank()) {
            log.warn("Push notification skipped: no message body provided");
            return;
        }

        try {
            // Get all active device tokens for the user
            List<UserDeviceToken> deviceTokens = deviceTokenRepository.findByUserIdAndActiveTrue(request.getUserId());

            if (deviceTokens.isEmpty()) {
                log.debug("No active device tokens found for user {}", request.getUserId());
                return;
            }

            // Send to all user devices
            List<String> tokens = deviceTokens.stream()
                    .map(UserDeviceToken::getDeviceToken)
                    .collect(Collectors.toList());

            sendToMultipleDevices(request, tokens);
            log.info("Push notification sent to {} devices for user {}", tokens.size(), request.getUserId());

        } catch (Exception e) {
            log.error("Failed to send push notification to user {}: {}",
                    request.getUserId(), e.getMessage(), e);
            throw e; // Re-throw to trigger DLQ handling
        }
    }

    /**
     * Send push notification to multiple devices, routing by token format:
     * Expo tokens go through the Expo Push API, raw FCM tokens through Firebase.
     */
    private void sendToMultipleDevices(NotificationRequest request, List<String> tokens) {
        Map<String, String> data = buildDataPayload(request);

        List<String> expoTokens = tokens.stream()
                .filter(ExpoPushService::isExpoToken)
                .collect(Collectors.toList());
        List<String> fcmTokens = tokens.stream()
                .filter(t -> !ExpoPushService.isExpoToken(t))
                .collect(Collectors.toList());

        if (!expoTokens.isEmpty()) {
            expoPushService.send(request.getSubject(), request.getBody(), data, expoTokens);
        }
        if (!fcmTokens.isEmpty()) {
            sendFcm(request, fcmTokens, data);
        }
    }

    /**
     * Send to raw FCM/APNs registration tokens via Firebase.
     */
    private void sendFcm(NotificationRequest request, List<String> tokens, Map<String, String> data) {
        if (firebaseMessaging == null) {
            log.warn("Firebase not configured, logging {} FCM push notification(s) instead", tokens.size());
            logPushNotification(request, tokens);
            return;
        }

        try {
            // Build the notification
            Notification notification = Notification.builder()
                    .setTitle(request.getSubject())
                    .setBody(request.getBody())
                    .build();

            // Configure Android specific options
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setSound("default")
                            .setClickAction("OPEN_NOTIFICATION")
                            .build())
                    .build();

            // Configure iOS specific options
            ApnsConfig apnsConfig = ApnsConfig.builder()
                    .setAps(Aps.builder()
                            .setSound("default")
                            .setBadge(1)
                            .build())
                    .build();

            // Send to multiple devices
            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(notification)
                    .putAllData(data)
                    .setAndroidConfig(androidConfig)
                    .setApnsConfig(apnsConfig)
                    .addAllTokens(tokens)
                    .build();

            BatchResponse response = firebaseMessaging.sendEachForMulticast(message);

            // Handle failed tokens
            if (response.getFailureCount() > 0) {
                handleFailedTokens(tokens, response.getResponses());
            }

            log.debug("FCM response: {} success, {} failures",
                    response.getSuccessCount(), response.getFailureCount());

        } catch (FirebaseMessagingException e) {
            log.error("Firebase messaging error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send FCM notification", e);
        }
    }

    /**
     * Build data payload for the push notification.
     */
    private Map<String, String> buildDataPayload(NotificationRequest request) {
        var dataBuilder = new java.util.HashMap<String, String>();

        dataBuilder.put("type", "notification");

        if (request.getReferenceId() != null) {
            dataBuilder.put("referenceId", request.getReferenceId());
        }
        if (request.getReferenceType() != null) {
            dataBuilder.put("referenceType", request.getReferenceType());
        }
        if (request.getChannel() != null) {
            dataBuilder.put("channel", request.getChannel());
        }

        // Add template data if present
        if (request.getTemplateData() != null) {
            request.getTemplateData().forEach((key, value) -> {
                if (value != null) {
                    dataBuilder.put(key, value.toString());
                }
            });
        }

        return dataBuilder;
    }

    /**
     * Handle failed tokens - deactivate invalid tokens.
     */
    private void handleFailedTokens(List<String> tokens, List<SendResponse> responses) {
        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);
            if (!response.isSuccessful()) {
                String token = tokens.get(i);
                FirebaseMessagingException exception = response.getException();

                if (exception != null) {
                    MessagingErrorCode errorCode = exception.getMessagingErrorCode();

                    // Deactivate invalid or unregistered tokens
                    if (errorCode == MessagingErrorCode.INVALID_ARGUMENT ||
                            errorCode == MessagingErrorCode.UNREGISTERED) {
                        log.info("Deactivating invalid FCM token: {}", token.substring(0, Math.min(20, token.length())));
                        deviceTokenRepository.deactivateToken(token);
                    } else {
                        log.warn("FCM error for token: {} - {}",
                                token.substring(0, Math.min(20, token.length())),
                                exception.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Log push notification when Firebase is not configured.
     */
    private void logPushNotification(NotificationRequest request, List<String> tokens) {
        log.info("Push notification (Firebase not configured):\n" +
                        "  UserId: {}\n" +
                        "  Devices: {}\n" +
                        "  Title: {}\n" +
                        "  Body: {}",
                request.getUserId(),
                tokens.size(),
                request.getSubject(),
                request.getBody() != null && request.getBody().length() > 100
                        ? request.getBody().substring(0, 100) + "..."
                        : request.getBody());
    }
}
