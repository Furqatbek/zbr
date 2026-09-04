package com.fooddelivery.notification.consumer;

import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.notification.dto.NotificationRequest;
import com.fooddelivery.notification.entity.UserDeviceToken;
import com.fooddelivery.notification.repository.UserDeviceTokenRepository;
import com.fooddelivery.notification.service.ApnsPushService;
import com.fooddelivery.notification.service.ExpoPushService;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fooddelivery.notification.service.PushAppIdResolver;
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
    private final PushAppIdResolver appIdResolver;
    private final ExpoPushService expoPushService;
    private final ApnsPushService apnsPushService;

    @Autowired(required = false)
    private FirebaseMessaging firebaseMessaging;

    /** Must match the channel the Android app creates (bump together, e.g. orders_v3). */
    @org.springframework.beans.factory.annotation.Value("${app.push.android.channel-id:orders_v2}")
    private String androidChannelId;

    /** Sound resource name WITHOUT the file extension. */
    @org.springframework.beans.factory.annotation.Value("${app.push.android.sound:new_order}")
    private String androidSound;

    /**
     * Process push notification requests from the queue.
     */
    // concurrency is set HERE rather than on the container factory on purpose.
    // That factory is shared by eleven listeners, including
    // order.status.changed and courier.location, where a second consumer would
    // let two updates for the same order be applied out of order. Push has no
    // such ordering requirement — each message is independent — so it is the
    // one queue that can safely be widened.
    //
    // The default was a single consumer, which serialised every push for all
    // three apps: one message with a slow or unreachable device (APNs allows
    // 10s per device) held up every other user's notification behind it.
    @RabbitListener(
            queues = RabbitMQConfig.NOTIFICATION_PUSH_QUEUE,
            id = "pushNotificationListener",
            containerFactory = "rabbitListenerContainerFactory",
            concurrency = "${app.push.consumer-concurrency:3-10}"
    )
    public void handlePushNotification(NotificationRequest request) {
        long receivedAt = System.currentTimeMillis();
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

            // One person is one user row across all three apps, so a courier
            // alert would otherwise also land on that same person's customer
            // app. Fails open — see PushAppIdResolver — so an unconfigured role
            // or a token without an appId still receives.
            int before = deviceTokens.size();
            deviceTokens = appIdResolver.filter(deviceTokens, request.getTargetRole());
            if (deviceTokens.isEmpty()) {
                log.info("Push for user {} dropped: none of their {} device(s) belong to the {} app",
                        request.getUserId(), before, request.getTargetRole());
                return;
            }

            sendToMultipleDevices(request, deviceTokens);
            // "handed off", not "sent": the per-provider result is logged by
            // each provider. This line only means the consumer got that far.
            log.info("Push handed off to {} device(s) for user {} in {}ms",
                    deviceTokens.size(), request.getUserId(),
                    System.currentTimeMillis() - receivedAt);

        } catch (Exception e) {
            log.error("Failed to send push notification to user {}: {}",
                    request.getUserId(), e.getMessage(), e);
            throw e; // Re-throw to trigger DLQ handling
        }
    }

    /**
     * Fan a notification out to a user's devices, routing per device:
     * iOS -> APNs (HTTP/2 + .p8), Android/unknown raw tokens -> FCM, and any
     * ExponentPushToken[...] -> the Expo Push API (apps still on Expo tokens).
     */
    private void sendToMultipleDevices(NotificationRequest request, List<UserDeviceToken> devices) {
        Map<String, String> data = buildDataPayload(request);

        // Expo tokens are identified by format regardless of the registered platform.
        List<String> expoTokens = devices.stream()
                .map(UserDeviceToken::getDeviceToken)
                .filter(ExpoPushService::isExpoToken)
                .collect(Collectors.toList());

        // Passed as entities, not raw strings: each carries its own appId, which
        // becomes the apns-topic so one .p8 team key can serve all three apps.
        List<UserDeviceToken> apnsDevices = devices.stream()
                .filter(d -> !ExpoPushService.isExpoToken(d.getDeviceToken()))
                .filter(d -> d.getDeviceType() == UserDeviceToken.DeviceType.IOS)
                .collect(Collectors.toList());

        List<String> fcmTokens = devices.stream()
                .filter(d -> !ExpoPushService.isExpoToken(d.getDeviceToken()))
                .filter(d -> d.getDeviceType() != UserDeviceToken.DeviceType.IOS)
                .map(UserDeviceToken::getDeviceToken)
                .collect(Collectors.toList());

        if (!expoTokens.isEmpty()) {
            expoPushService.send(request.getSubject(), request.getBody(), data, expoTokens);
        }
        if (!apnsDevices.isEmpty()) {
            apnsPushService.send(request.getSubject(), request.getBody(), data, apnsDevices);
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

        long startedAt = System.currentTimeMillis();
        try {
            // Build the notification
            Notification notification = Notification.builder()
                    .setTitle(request.getSubject())
                    .setBody(request.getBody())
                    .build();

            // Android: HIGH priority is mandatory to bypass Doze (normal priority
            // batches and arrives minutes late). Channel id must match the channel
            // the app creates at start-up; sound omits the file extension.
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setChannelId(androidChannelId)
                            .setSound(androidSound)
                            .setPriority(AndroidNotification.Priority.MAX)
                            .setVisibility(AndroidNotification.Visibility.PUBLIC)
                            .setDefaultVibrateTimings(false)
                            .setVibrateTimingsInMillis(new long[]{0L, 400L, 200L, 400L})
                            .setClickAction("OPEN_NOTIFICATION")
                            .build())
                    .build();

            // iOS via FCM is only a fallback for legacy tokens registered without a
            // platform; native APNs delivery is handled by ApnsPushService.
            ApnsConfig apnsConfig = ApnsConfig.builder()
                    .putHeader("apns-push-type", "alert")
                    .putHeader("apns-priority", "10")
                    .setAps(Aps.builder()
                            .setSound("new_order.wav")
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

            // INFO, not DEBUG: this is the only line that says whether Google
            // ACCEPTED the message. Without it the log claims a push was "sent"
            // even when FCM rejected every token, which makes a delivery
            // complaint impossible to triage — the backend looks healthy either
            // way. One line per push is affordable.
            log.info("FCM accepted {} of {} token(s) in {}ms",
                    response.getSuccessCount(), tokens.size(),
                    System.currentTimeMillis() - startedAt);

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

        // The apps deep-link on data.orderId and switch UI on data.type, so both
        // must be present. orderId MUST be a bare numeric string — the clients
        // validate it before navigating and drop anything else.
        if ("ORDER".equalsIgnoreCase(request.getReferenceType()) && request.getReferenceId() != null) {
            String orderId = request.getReferenceId().trim();
            if (orderId.matches("\\d+")) {
                dataBuilder.put("orderId", orderId);
            } else {
                log.warn("Not setting data.orderId — reference id '{}' is not numeric", orderId);
            }
        }
        // A caller-supplied notification type (e.g. NEW_ORDER_RECEIVED) wins over
        // the generic default; templateData may already have set it above.
        if (request.getTemplateId() != null && !request.getTemplateId().isBlank()
                && "notification".equals(dataBuilder.get("type"))) {
            dataBuilder.put("type", request.getTemplateId());
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
                        deviceTokenRepository.deactivateRejectedToken(token);
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
