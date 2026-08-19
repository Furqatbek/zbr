package com.fooddelivery.sms.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.notification.dto.NotificationRequest;
import com.fooddelivery.sms.dto.SmsMessage;
import com.fooddelivery.sms.dto.SmsSendResponse;
import com.fooddelivery.sms.entity.SmsTemplate;
import com.fooddelivery.sms.entity.SmsTemplate.SmsTemplateType;
import com.fooddelivery.sms.service.SmsProviderFactory;
import com.fooddelivery.sms.service.SmsTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final SmsTemplateService smsTemplateService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final com.fooddelivery.sms.config.SmsProperties smsProperties;

    private static final int MAX_RETRIES = 3;
    private static final Pattern OTP_CODE_PATTERN = Pattern.compile("\\b(\\d{4,6})\\b");

    /**
     * SMS is reserved for auth/security codes the user is actively waiting for.
     * Business notifications (orders, delivery, promos, welcome) are delivered by
     * push + WebSocket only, so anything outside this set is dropped.
     */
    private static final Set<SmsMessage.SmsType> AUTH_SMS_TYPES =
            EnumSet.of(SmsMessage.SmsType.OTP, SmsMessage.SmsType.PASSWORD_RESET);

    /**
     * Process SMS notification requests from the queue.
     */
    @RabbitListener(
            queues = RabbitMQConfig.NOTIFICATION_SMS_QUEUE,
            id = "smsNotificationListener",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleSmsNotification(Object payload) {
        // Handle raw Message type (fallback when automatic deserialization fails)
        if (payload instanceof Message message) {
            payload = deserializeMessage(message);
            if (payload == null) {
                return;
            }
        }

        // SMS is reserved for auth/security codes (OTP, password reset). Everything
        // else — order confirmations, status updates, delivery updates, welcome
        // messages — is delivered by push + WebSocket only.
        if (payload instanceof SmsMessage smsMessage) {
            if (!AUTH_SMS_TYPES.contains(smsMessage.getType())) {
                log.debug("Dropping non-auth SMS (type={}): notifications go out via push only",
                        smsMessage.getType());
                return;
            }
            handleSmsRetry(smsMessage);
            return;
        }

        // NotificationRequest on this queue is always a business-notification
        // fan-out. Drop it — this also drains any stale messages left in the
        // durable queue from before SMS notifications were switched off.
        if (payload instanceof NotificationRequest request) {
            log.debug("Dropping SMS notification request (subject={}): notifications go out via push only",
                    request.getSubject());
            return;
        }

        log.warn("Unknown message type received: {}", payload.getClass().getName());
        return;
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
            // SMS globally disabled — discard the message (do not requeue, do not DLQ)
            if (!smsProperties.isEnabled()) {
                log.info("SMS is disabled, discarding message: phone={}",
                        maskPhone(smsMessage.getPhoneNumber()));
                return;
            }

            if (!smsProviderFactory.isAnyProviderAvailable()) {
                // Provider temporarily unavailable — increment retry so it eventually
                // reaches the DLQ instead of requeuing forever.
                if (currentRetry < MAX_RETRIES) {
                    log.warn("No SMS provider available, requeueing (attempt {}/{})",
                            currentRetry + 1, MAX_RETRIES);
                    requeueMessage(smsMessage, currentRetry + 1);
                } else {
                    log.error("No SMS provider available after {} retries, moving to DLQ: phone={}",
                            MAX_RETRIES, maskPhone(smsMessage.getPhoneNumber()));
                    sendToDlq(smsMessage);
                }
                return;
            }

            // For OTP messages, try to use template-based content
            SmsMessage messageToSend = transformOtpMessageIfNeeded(smsMessage);

            SmsSendResponse response = smsProviderFactory.sendSms(messageToSend);

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
     * Mask phone number for logging.
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return "***";
        }
        return phone.substring(0, 4) + "****" + phone.substring(phone.length() - 2);
    }

    /**
     * Manually deserialize a raw AMQP Message when automatic conversion fails.
     */
    private Object deserializeMessage(Message message) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            String typeId = message.getMessageProperties().getHeader("__TypeId__");

            if (typeId == null) {
                log.warn("Message missing __TypeId__ header, attempting to parse as NotificationRequest");
                return objectMapper.readValue(body, NotificationRequest.class);
            }

            if (typeId.contains("SmsMessage")) {
                return objectMapper.readValue(body, SmsMessage.class);
            } else if (typeId.contains("NotificationRequest")) {
                return objectMapper.readValue(body, NotificationRequest.class);
            } else {
                log.warn("Unknown TypeId in message: {}", typeId);
                // Try NotificationRequest as default
                return objectMapper.readValue(body, NotificationRequest.class);
            }
        } catch (Exception e) {
            log.error("Failed to deserialize message manually: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Transform OTP messages to use template-based content if template is available.
     * This handles legacy messages in the queue that have hardcoded content.
     */
    private SmsMessage transformOtpMessageIfNeeded(SmsMessage smsMessage) {
        // Only transform OTP type messages
        if (smsMessage.getType() != SmsMessage.SmsType.OTP) {
            return smsMessage;
        }

        try {
            // Find approved OTP template
            SmsTemplate template = smsTemplateService.getApprovedTemplateForSending(SmsTemplateType.OTP);
            if (template == null) {
                log.debug("No approved OTP template found, using original message");
                return smsMessage;
            }

            // Extract OTP code from the original message
            String otpCode = extractOtpCode(smsMessage.getMessage());
            if (otpCode == null) {
                log.warn("Could not extract OTP code from message, using original");
                return smsMessage;
            }

            // Substitute variables in template
            String templatedMessage = template.getContent().replace("{code}", otpCode);

            log.info("Transformed OTP message to use template: {}", template.getTemplateCode());

            return SmsMessage.builder()
                    .messageId(smsMessage.getMessageId())
                    .phoneNumber(smsMessage.getPhoneNumber())
                    .message(templatedMessage)
                    .type(smsMessage.getType())
                    .referenceId(smsMessage.getReferenceId())
                    .referenceType(smsMessage.getReferenceType())
                    .retryCount(smsMessage.getRetryCount())
                    .createdAt(smsMessage.getCreatedAt())
                    .priority(smsMessage.getPriority())
                    .build();

        } catch (Exception e) {
            log.error("Error transforming OTP message: {}", e.getMessage());
            return smsMessage;
        }
    }

    /**
     * Extract OTP code (4-6 digit number) from message content.
     */
    private String extractOtpCode(String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = OTP_CODE_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
