package com.fooddelivery.sms.service;

import com.fooddelivery.common.config.RabbitMQConfig;
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
     * Text used only when no APPROVED template exists for the type. Configurable
     * so no user-facing wording is compiled into the backend — the platform
     * operates in Uzbek and an English string baked into a jar cannot be fixed
     * without a redeploy. {code} is substituted; everything else is sent as-is.
     * The template API remains the primary mechanism; these are the safety net.
     */
    @org.springframework.beans.factory.annotation.Value("${app.sms.default-text.otp:Your verification code is: {code}}")
    private String otpText = "Your verification code is: {code}";

    @org.springframework.beans.factory.annotation.Value("${app.sms.default-text.password-reset:Your password reset code: {code}}")
    private String passwordResetText = "Your password reset code: {code}";

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
        String message = otpText.replace("{code}", otpCode);

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

            // Use provider's templated SMS endpoint with template ID
            // This allows the provider to use their registered/approved template
            String providerTemplateId = template.getProviderTemplateId();
            if (providerTemplateId != null && !providerTemplateId.isBlank()) {
                SmsSendResponse response = provider.sendTemplatedSms(
                        phoneNumber,
                        providerTemplateId,
                        Map.of("code", otpCode)
                );

                if (response.isSuccess()) {
                    log.info("OTP sent via templated endpoint {} to phone: {}",
                            template.getTemplateCode(), maskPhone(phoneNumber));
                    return true;
                } else {
                    log.warn("Failed to send OTP via templated endpoint: {}, falling back to raw message",
                            response.getMessage());
                }
            }

            // Fallback: substitute variables locally and send as raw message
            String messageContent = substituteTemplateVariables(template.getContent(), Map.of("code", otpCode));

            SmsMessage smsMessage = SmsMessage.builder()
                    .messageId(UUID.randomUUID().toString())
                    .phoneNumber(phoneNumber)
                    .message(messageContent)
                    .type(SmsMessage.SmsType.OTP)
                    .priority(10)
                    .build();

            SmsSendResponse response = provider.sendSms(smsMessage);

            if (response.isSuccess()) {
                log.info("OTP sent via raw message {} to phone: {}",
                        template.getTemplateCode(), maskPhone(phoneNumber));
                return true;
            } else {
                log.warn("Failed to send OTP via raw message: {}", response.getMessage());
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
     * Send password reset code SMS.
     */
    @Async("notificationExecutor")
    public void sendPasswordResetCode(String phoneNumber, String resetCode) {
        if (!smsProperties.isEnabled() || phoneNumber == null) {
            return;
        }

        String message = passwordResetText.replace("{code}", resetCode);

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
