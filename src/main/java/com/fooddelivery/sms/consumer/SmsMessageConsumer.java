package com.fooddelivery.sms.consumer;

import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.notification.dto.NotificationRequest;
import com.fooddelivery.sms.dto.SmsMessage;
import com.fooddelivery.sms.dto.SmsSendResponse;
import com.fooddelivery.sms.service.SmsProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consumer for SMS messages from RabbitMQ queue.
 * Processes SMS notifications and sends them via configured SMS provider.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.sms", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SmsMessageConsumer {

    private final SmsProviderFactory smsProviderFactory;
    private final RabbitTemplate rabbitTemplate;

    private static final int MAX_RETRIES = 3;

    /**
     * Process SMS notification requests from the queue.
     */
    @RabbitListener(
            queues = RabbitMQConfig.NOTIFICATION_SMS_QUEUE,
            id = "smsNotificationListener",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleSmsNotification(Object payload) {
        // Handle both NotificationRequest (new messages) and SmsMessage (retries)
        if (payload instanceof SmsMessage smsMessage) {
            handleSmsRetry(smsMessage);
            return;
        }

        if (!(payload instanceof NotificationRequest request)) {
            log.warn("Unknown message type received: {}", payload.getClass().getName());
            return;
        }

        log.info("Received SMS notification request: phone={}, subject={}",
                maskPhone(request.getPhone()), request.getSubject());

        if (request.getPhone() == null || request.getPhone().isBlank()) {
            log.warn("SMS notification skipped: no phone number provided");
            return;
        }

        if (request.getBody() == null || request.getBody().isBlank()) {
            log.warn("SMS notification skipped: no message body provided");
            return;
        }

        try {
            SmsMessage smsMessage = SmsMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .phoneNumber(request.getPhone())
                    .message(buildSmsContent(request))
                    .type(determineSmsType(request))
                    .referenceId(request.getReferenceId())
                    .referenceType(request.getReferenceType())
                    .priority(request.getPriority() != null ? request.getPriority() : 5)
                    .build();

            sendWithRetry(smsMessage, 0);

        } catch (Exception e) {
            log.error("Failed to process SMS notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle retried SMS messages.
     */
    private void handleSmsRetry(SmsMessage smsMessage) {
        log.info("Processing SMS retry {}/{}: phone={}",
                smsMessage.getRetryCount(), MAX_RETRIES, maskPhone(smsMessage.getPhoneNumber()));
        sendWithRetry(smsMessage, smsMessage.getRetryCount());
    }

    /**
     * Send SMS with retry logic.
     */
    private void sendWithRetry(SmsMessage smsMessage, int currentRetry) {
        try {
            if (!smsProviderFactory.isAnyProviderAvailable()) {
                log.warn("No SMS provider is available, message will be requeued");
                requeueMessage(smsMessage, currentRetry);
                return;
            }

            SmsSendResponse response = smsProviderFactory.sendSms(smsMessage);

            if (response.isSuccess()) {
                log.info("SMS sent successfully via {}: id={}, phone={}",
                        response.getProvider(),
                        response.getSmsId(),
                        maskPhone(smsMessage.getPhoneNumber()));
            } else {
                log.warn("SMS send failed via {}: error={}, code={}",
                        response.getProvider(),
                        response.getMessage(),
                        response.getErrorCode());

                if (currentRetry < MAX_RETRIES) {
                    requeueMessage(smsMessage, currentRetry + 1);
                } else {
                    log.error("Max retries exceeded for SMS to {}, moving to DLQ",
                            maskPhone(smsMessage.getPhoneNumber()));
                    sendToDlq(smsMessage);
                }
            }

        } catch (Exception e) {
            log.error("Error sending SMS (attempt {}): {}", currentRetry + 1, e.getMessage());

            if (currentRetry < MAX_RETRIES) {
                requeueMessage(smsMessage, currentRetry + 1);
            } else {
                log.error("Max retries exceeded for SMS to {}, moving to DLQ",
                        maskPhone(smsMessage.getPhoneNumber()));
                sendToDlq(smsMessage);
            }
        }
    }

    /**
     * Requeue message for retry with incremented retry count.
     */
    private void requeueMessage(SmsMessage smsMessage, int newRetryCount) {
        SmsMessage retryMessage = SmsMessage.builder()
                .messageId(smsMessage.getMessageId())
                .phoneNumber(smsMessage.getPhoneNumber())
                .message(smsMessage.getMessage())
                .type(smsMessage.getType())
                .referenceId(smsMessage.getReferenceId())
                .referenceType(smsMessage.getReferenceType())
                .retryCount(newRetryCount)
                .createdAt(smsMessage.getCreatedAt())
                .priority(smsMessage.getPriority())
                .build();

        try {
            Thread.sleep(1000L * (long) Math.pow(2, newRetryCount)); // Exponential backoff
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_SMS_KEY,
                retryMessage
        );

        log.info("SMS requeued for retry {}/{}: phone={}",
                newRetryCount, MAX_RETRIES, maskPhone(smsMessage.getPhoneNumber()));
    }

    /**
     * Send failed message to Dead Letter Queue.
     */
    private void sendToDlq(SmsMessage smsMessage) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DLX_EXCHANGE,
                "dlq",
                smsMessage
        );
    }

    /**
     * Build SMS content from notification request.
     */
    private String buildSmsContent(NotificationRequest request) {
        StringBuilder content = new StringBuilder();

        if (request.getSubject() != null && !request.getSubject().isBlank()) {
            content.append(request.getSubject()).append("\n");
        }

        content.append(request.getBody());

        // SMS character limit consideration (160 for single SMS, 153 for multi-part)
        String message = content.toString();
        if (message.length() > 450) {
            message = message.substring(0, 447) + "...";
        }

        return message;
    }

    /**
     * Determine SMS type from notification request.
     */
    private SmsMessage.SmsType determineSmsType(NotificationRequest request) {
        if (request.getReferenceType() == null) {
            return SmsMessage.SmsType.GENERAL;
        }

        return switch (request.getReferenceType().toUpperCase()) {
            case "ORDER" -> SmsMessage.SmsType.ORDER_STATUS_UPDATE;
            case "ORDER_CONFIRMATION" -> SmsMessage.SmsType.ORDER_CONFIRMATION;
            case "PAYMENT" -> SmsMessage.SmsType.PAYMENT_CONFIRMATION;
            case "DELIVERY" -> SmsMessage.SmsType.DELIVERY_UPDATE;
            case "OTP" -> SmsMessage.SmsType.OTP;
            case "PASSWORD_RESET" -> SmsMessage.SmsType.PASSWORD_RESET;
            case "WELCOME" -> SmsMessage.SmsType.WELCOME;
            default -> SmsMessage.SmsType.GENERAL;
        };
    }

    /**
     * Mask phone number for logging.
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return "***";
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }
}
