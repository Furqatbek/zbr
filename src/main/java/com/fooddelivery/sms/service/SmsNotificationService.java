package com.fooddelivery.sms.service;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.notification.dto.NotificationRequest;
import com.fooddelivery.sms.config.EskizSmsProperties;
import com.fooddelivery.sms.dto.SmsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
    private final EskizSmsProperties smsProperties;

    /**
     * Send OTP code via SMS.
     */
    @Async("notificationExecutor")
    public void sendOtp(String phoneNumber, String otpCode) {
        if (!smsProperties.isEnabled()) {
            log.debug("SMS disabled, skipping OTP");
            return;
        }

        String message = String.format("Your verification code is: %s\nValid for 5 minutes.\n- Food Delivery", otpCode);

        SmsMessage smsMessage = SmsMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .phoneNumber(phoneNumber)
                .message(message)
                .type(SmsMessage.SmsType.OTP)
                .priority(10) // High priority for OTP
                .build();

        queueSmsMessage(smsMessage);
        log.info("OTP SMS queued for phone: {}", maskPhone(phoneNumber));
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
