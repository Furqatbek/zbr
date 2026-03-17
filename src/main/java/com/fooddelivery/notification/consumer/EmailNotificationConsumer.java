package com.fooddelivery.notification.consumer;

import com.fooddelivery.common.config.RabbitMQConfig;
import com.fooddelivery.notification.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumer for email notification requests from RabbitMQ.
 * Processes email notifications and sends them via configured email provider.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.email", name = "enabled", havingValue = "true", matchIfMissing = true)
public class EmailNotificationConsumer {

    /**
     * Process email notification requests from the queue.
     */
    @RabbitListener(
            queues = RabbitMQConfig.NOTIFICATION_EMAIL_QUEUE,
            id = "emailNotificationListener",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleEmailNotification(NotificationRequest request) {
        log.info("Consuming email notification from queue: to={}, subject={}",
                maskEmail(request.getEmail()), request.getSubject());

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            log.warn("Email notification skipped: no email address provided");
            return;
        }

        if (request.getBody() == null || request.getBody().isBlank()) {
            log.warn("Email notification skipped: no message body provided");
            return;
        }

        try {
            // TODO: Integrate with actual email provider (SendGrid, SES, etc.)
            sendEmail(request);
            log.info("Email notification sent successfully to: {}", maskEmail(request.getEmail()));

        } catch (Exception e) {
            log.error("Failed to send email notification to {}: {}",
                    maskEmail(request.getEmail()), e.getMessage(), e);
            throw e; // Re-throw to trigger DLQ handling
        }
    }

    /**
     * Send email via configured provider.
     * Placeholder for actual email provider integration.
     */
    private void sendEmail(NotificationRequest request) {
        // Log email details for now
        log.info("Sending email:\n  To: {}\n  Subject: {}\n  Body: {}...",
                request.getEmail(),
                request.getSubject(),
                request.getBody().length() > 100 ?
                        request.getBody().substring(0, 100) : request.getBody());

        // TODO: Implement actual email sending
        // Example integration points:
        // - Spring Mail (JavaMailSender)
        // - SendGrid
        // - Amazon SES
        // - Mailgun
    }

    /**
     * Mask email for logging.
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return "***" + email.substring(atIndex);
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
