package com.fooddelivery.sms.service;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.notification.dto.NotificationRequest;
import com.fooddelivery.sms.config.SmsProperties;
import com.fooddelivery.sms.dto.SmsMessage;
import com.fooddelivery.sms.dto.SmsSendResponse;
import com.fooddelivery.sms.entity.SmsTemplate;
import com.fooddelivery.sms.entity.SmsTemplate.SmsTemplateType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Service for sending SMS notifications.
 * Publishes messages to RabbitMQ for async processing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsNotificationService {

    private final RabbitTemplate rabbitTemplate;
    private final SmsProperties smsProperties;
    private final SmsTemplateService smsTemplateService;
    private final SmsProviderFactory smsProviderFactory;

    /**
     * Send OTP code via SMS.
     * First tries to use an approved OTP template, falls back to hardcoded message if not available.
     */
    @Async("notificationExecutor")
    public void sendOtp(String phoneNumber, String otpCode) {
        if (!smsProperties.isEnabled()) {
            log.debug("SMS disabled, skipping OTP");
            return;
        }

        // Try to use OTP template first
        if (trySendOtpWithTemplate(phoneNumber, otpCode)) {
            return;
        }

        // Fallback to hardcoded message if template not available
        String message = String.format("Your verification code is: %s\nValid for 5 minutes.\n- Food Delivery", otpCode);

        SmsMessage smsMessage = SmsMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .phoneNumber(phoneNumber)
                .message(message)
                .type(SmsMessage.SmsType.OTP)
                .priority(10) // High priority for OTP
                .build();

        queueSmsMessage(smsMessage);
        log.info("OTP SMS queued for phone: {} (using fallback message)", maskPhone(phoneNumber));
    }

    /**
     * Try to send OTP using approved template.
     * Returns true if successfully sent via template, false otherwise.
     */
    private boolean trySendOtpWithTemplate(String phoneNumber, String otpCode) {
        try {
            // Find approved OTP template for current provider
            SmsTemplate template = smsTemplateService.getApprovedTemplateForSending(SmsTemplateType.OTP);

            if (template == null) {
                log.debug("No approved OTP template found, will use fallback message");
                return false;
            }

            // Get the provider and send using template
            SmsProvider provider = smsProviderFactory.getPrimaryProvider();
            if (provider == null || !provider.isAvailable()) {
                log.warn("Primary SMS provider not available for templated OTP");
                return false;
            }

            // Substitute variables in template content ourselves
            // DevSMS/Eskiz doesn't have a template API - we need to send the final message
            String messageContent = substituteTemplateVariables(template.getContent(), Map.of("code", otpCode));

            // Send using regular sendSms with the substituted message
            SmsMessage smsMessage = SmsMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .phoneNumber(phoneNumber)
                    .message(messageContent)
                    .type(SmsMessage.SmsType.OTP)
                    .priority(10)
                    .build();

            SmsSendResponse response = provider.sendSms(smsMessage);

            if (response.isSuccess()) {
                log.info("OTP sent via template {} to phone: {}",
                        template.getTemplateCode(), maskPhone(phoneNumber));
                return true;
            } else {
                log.warn("Failed to send OTP via template: {}", response.getMessage());
                return false;
            }

        } catch (Exception e) {
            log.error("Error sending OTP via template: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Substitute variables in template content.
     * Variables format: {variable_name}
     */
    private String substituteTemplateVariables(String templateContent, Map<String, String> variables) {
        String result = templateContent;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /**
     * Send welcome SMS to new user.
     */
    @Async("notificationExecutor")
    public void sendWelcomeSms(User user) {
        if (!smsProperties.isEnabled() || user.getPhone() == null) {
            return;
        }

        String message = String.format(
                "Welcome to Food Delivery, %s! Start ordering delicious food from your favorite restaurants.",
                user.getFirstName() != null ? user.getFirstName() : "there"
        );

        NotificationRequest request = NotificationRequest.builder()
                .userId(user.getId())
                .phone(user.getPhone())
                .subject("Welcome!")
                .body(message)
                .channel("sms")
                .referenceType("WELCOME")
                .build();

        queueNotification(request);
        log.info("Welcome SMS queued for user: {}", user.getId());
    }

    /**
     * Send order confirmation SMS.
     */
    @Async("notificationExecutor")
    public void sendOrderConfirmation(String phoneNumber, String orderNo, String restaurantName, String totalAmount) {
        if (!smsProperties.isEnabled() || phoneNumber == null) {
            return;
        }

        String message = String.format(
                "Order %s confirmed!\nRestaurant: %s\nTotal: %s\nTrack your order in the app.",
                orderNo, restaurantName, totalAmount
        );

        NotificationRequest request = NotificationRequest.builder()
                .phone(phoneNumber)
                .subject("Order Confirmed")
                .body(message)
                .channel("sms")
                .referenceId(orderNo)
                .referenceType("ORDER_CONFIRMATION")
                .priority(8)
                .build();

        queueNotification(request);
        log.info("Order confirmation SMS queued: orderNo={}", orderNo);
    }

    /**
     * Send order status update SMS.
     */
    @Async("notificationExecutor")
    public void sendOrderStatusUpdate(String phoneNumber, String orderNo, String status, String details) {
        if (!smsProperties.isEnabled() || phoneNumber == null) {
            return;
        }

        String message = String.format(
                "Order %s: %s%s",
                orderNo,
                status,
                details != null ? "\n" + details : ""
        );

        NotificationRequest request = NotificationRequest.builder()
                .phone(phoneNumber)
                .subject("Order Update")
                .body(message)
                .channel("sms")
                .referenceId(orderNo)
                .referenceType("ORDER")
                .priority(7)
                .build();

        queueNotification(request);
        log.info("Order status SMS queued: orderNo={}, status={}", orderNo, status);
    }

    /**
     * Send delivery update SMS.
     */
    @Async("notificationExecutor")
    public void sendDeliveryUpdate(String phoneNumber, String orderNo, String courierName, String estimatedTime) {
        if (!smsProperties.isEnabled() || phoneNumber == null) {
            return;
        }

        String message = String.format(
                "Order %s is on the way!\nCourier: %s\nETA: %s",
                orderNo,
                courierName != null ? courierName : "Your courier",
                estimatedTime != null ? estimatedTime : "Soon"
        );

        NotificationRequest request = NotificationRequest.builder()
                .phone(phoneNumber)
                .subject("Out for Delivery")
                .body(message)
                .channel("sms")
                .referenceId(orderNo)
                .referenceType("DELIVERY")
                .priority(8)
                .build();

        queueNotification(request);
        log.info("Delivery update SMS queued: orderNo={}", orderNo);
    }

    /**
     * Send payment confirmation SMS.
     */
    @Async("notificationExecutor")
    public void sendPaymentConfirmation(String phoneNumber, String orderNo, String amount) {
        if (!smsProperties.isEnabled() || phoneNumber == null) {
            return;
        }

        String message = String.format(
                "Payment received for order %s.\nAmount: %s\nThank you!",
                orderNo, amount
        );

        NotificationRequest request = NotificationRequest.builder()
                .phone(phoneNumber)
                .subject("Payment Confirmed")
                .body(message)
                .channel("sms")
                .referenceId(orderNo)
                .referenceType("PAYMENT")
                .priority(7)
                .build();

        queueNotification(request);
        log.info("Payment confirmation SMS queued: orderNo={}", orderNo);
    }

    /**
     * Send password reset code SMS.
     */
    @Async("notificationExecutor")
    public void sendPasswordResetCode(String phoneNumber, String resetCode) {
        if (!smsProperties.isEnabled() || phoneNumber == null) {
            return;
        }

        String message = String.format(
                "Your password reset code: %s\nValid for 1 hour.\nIf you didn't request this, ignore this message.",
                resetCode
        );

        SmsMessage smsMessage = SmsMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .phoneNumber(phoneNumber)
                .message(message)
                .type(SmsMessage.SmsType.PASSWORD_RESET)
                .priority(9)
                .build();

        queueSmsMessage(smsMessage);
        log.info("Password reset SMS queued for phone: {}", maskPhone(phoneNumber));
    }

    /**
     * Send custom SMS message.
     */
    @Async("notificationExecutor")
    public void sendCustomSms(String phoneNumber, String message, String referenceId, String referenceType) {
        if (!smsProperties.isEnabled() || phoneNumber == null) {
            return;
        }

        NotificationRequest request = NotificationRequest.builder()
                .phone(phoneNumber)
                .body(message)
                .channel("sms")
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();

        queueNotification(request);
        log.info("Custom SMS queued for phone: {}", maskPhone(phoneNumber));
    }

    /**
     * Queue notification request to RabbitMQ.
     */
    private void queueNotification(NotificationRequest request) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_SMS_KEY,
                request
        );
    }

    /**
     * Queue SMS message directly to RabbitMQ.
     */
    private void queueSmsMessage(SmsMessage smsMessage) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_SMS_KEY,
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
}
