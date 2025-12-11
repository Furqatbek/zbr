package com.fooddelivery.notification.service;

import com.fooddelivery.auth.entity.User;
import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.notification.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for sending notifications via various channels.
 * Supports email, SMS, and push notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final RabbitTemplate rabbitTemplate;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@fooddelivery.com}")
    private String fromEmail;

    @Value("${app.notification.enabled:true}")
    private boolean notificationsEnabled;

    /**
     * Send welcome email to new user.
     */
    @Async("notificationExecutor")
    public void sendWelcomeEmail(User user) {
        if (!notificationsEnabled) {
            log.debug("Notifications disabled, skipping welcome email");
            return;
        }

        try {
            NotificationRequest request = NotificationRequest.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .subject("Welcome to Food Delivery Platform!")
                    .body(String.format("""
                            Hi %s,

                            Welcome to Food Delivery Platform! We're excited to have you.

                            Start exploring restaurants and ordering delicious food today.

                            Best regards,
                            The Food Delivery Team
                            """, user.getFirstName() != null ? user.getFirstName() : "there"))
                    .channel("email")
                    .build();

            queueEmailNotification(request);
            log.info("Welcome email queued for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email: {}", e.getMessage());
        }
    }

    /**
     * Send password reset email.
     */
    @Async("notificationExecutor")
    public void sendPasswordResetEmail(User user, String token) {
        if (!notificationsEnabled) {
            return;
        }

        try {
            String resetLink = "https://fooddelivery.com/reset-password?token=" + token;

            NotificationRequest request = NotificationRequest.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .subject("Password Reset Request")
                    .body(String.format("""
                            Hi %s,

                            You requested to reset your password. Click the link below to reset it:

                            %s

                            This link will expire in 1 hour.

                            If you didn't request this, please ignore this email.

                            Best regards,
                            The Food Delivery Team
                            """, user.getFirstName() != null ? user.getFirstName() : "there", resetLink))
                    .channel("email")
                    .build();

            queueEmailNotification(request);
            log.info("Password reset email queued for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email: {}", e.getMessage());
        }
    }

    /**
     * Send order confirmation notification.
     */
    @Async("notificationExecutor")
    public void sendOrderConfirmation(String email, String orderNo, String restaurantName) {
        if (!notificationsEnabled) {
            return;
        }

        try {
            NotificationRequest request = NotificationRequest.builder()
                    .email(email)
                    .subject("Order Confirmed - " + orderNo)
                    .body(String.format("""
                            Your order %s has been confirmed!

                            Restaurant: %s

                            We'll notify you when your order is ready.

                            Track your order in the app.

                            Enjoy your meal!
                            """, orderNo, restaurantName))
                    .channel("email")
                    .build();

            queueEmailNotification(request);
        } catch (Exception e) {
            log.error("Failed to send order confirmation: {}", e.getMessage());
        }
    }

    /**
     * Send order status update notification.
     */
    @Async("notificationExecutor")
    public void sendOrderStatusUpdate(String email, String orderNo, String status, String message) {
        if (!notificationsEnabled) {
            return;
        }

        try {
            NotificationRequest request = NotificationRequest.builder()
                    .email(email)
                    .subject("Order Update - " + orderNo)
                    .body(String.format("""
                            Order %s status update: %s

                            %s

                            Track your order in the app.
                            """, orderNo, status, message != null ? message : ""))
                    .channel("email")
                    .build();

            queueEmailNotification(request);
        } catch (Exception e) {
            log.error("Failed to send order status update: {}", e.getMessage());
        }
    }

    /**
     * Queue email notification for async processing.
     */
    public void queueEmailNotification(NotificationRequest request) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_EMAIL_KEY,
                request
        );
    }

    /**
     * Queue SMS notification for async processing.
     */
    public void queueSmsNotification(NotificationRequest request) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_SMS_KEY,
                request
        );
    }

    /**
     * Queue push notification for async processing.
     */
    public void queuePushNotification(NotificationRequest request) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_PUSH_KEY,
                request
        );
    }

    /**
     * Send email directly (blocking).
     */
    public void sendEmailDirect(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent directly to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email directly: {}", e.getMessage());
        }
    }
}
