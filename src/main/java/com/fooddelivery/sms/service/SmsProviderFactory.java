package com.fooddelivery.sms.service;

import com.fooddelivery.sms.config.SmsProperties;
import com.fooddelivery.sms.config.SmsProperties.SmsProviderType;
import com.fooddelivery.sms.dto.SmsMessage;
import com.fooddelivery.sms.dto.SmsSendResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Factory for SMS providers.
 * Selects the appropriate provider based on configuration and handles fallback.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmsProviderFactory {

    private final SmsProperties smsProperties;
    private final Optional<EskizSmsClient> eskizClient;
    private final Optional<DevSmsClient> devSmsClient;

    @PostConstruct
    public void init() {
        log.info("SMS Provider Factory initialized. Primary provider: {}, Fallback enabled: {}",
                smsProperties.getProvider(),
                smsProperties.isFallbackEnabled());

        if (!smsProperties.isEnabled()) {
            log.warn("SMS sending is globally disabled");
            return;
        }

        // Log available providers
        eskizClient.ifPresent(c -> log.info("Eskiz SMS client available: {}", c.isAvailable()));
        devSmsClient.ifPresent(c -> log.info("DevSMS client available: {}", c.isAvailable()));
    }

    /**
     * Get the primary SMS provider based on configuration.
     */
    public SmsProvider getPrimaryProvider() {
        return getProvider(smsProperties.getProvider());
    }

    /**
     * Get the fallback SMS provider if configured.
     */
    public SmsProvider getFallbackProvider() {
        if (!smsProperties.isFallbackEnabled() || smsProperties.getFallbackProvider() == null) {
            return null;
        }
        return getProvider(smsProperties.getFallbackProvider());
    }

    /**
     * Get a specific provider by type.
     */
    public SmsProvider getProvider(SmsProviderType type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case ESKIZ -> eskizClient.orElse(null);
            case DEVSMS -> devSmsClient.orElse(null);
        };
    }

    /**
     * Send SMS using the configured provider with optional fallback.
     */
    public SmsSendResponse sendSms(SmsMessage smsMessage) {
        if (!smsProperties.isEnabled()) {
            log.warn("SMS sending is disabled globally");
            return SmsSendResponse.failure("SMS sending is disabled", "DISABLED", "NONE");
        }

        SmsProvider primary = getPrimaryProvider();

        if (primary == null || !primary.isAvailable()) {
            log.warn("Primary SMS provider {} is not available", smsProperties.getProvider());

            // Try fallback if enabled
            if (smsProperties.isFallbackEnabled()) {
                return sendWithFallback(smsMessage);
            }

            return SmsSendResponse.failure(
                    "No SMS provider available",
                    "NO_PROVIDER",
                    smsProperties.getProvider().name()
            );
        }

        try {
            SmsSendResponse response = primary.sendSms(smsMessage);

            if (!response.isSuccess() && smsProperties.isFallbackEnabled()) {
                log.warn("Primary provider failed, trying fallback...");
                return sendWithFallback(smsMessage);
            }

            return response;

        } catch (Exception e) {
            log.error("Primary SMS provider error: {}", e.getMessage());

            if (smsProperties.isFallbackEnabled()) {
                return sendWithFallback(smsMessage);
            }

            return SmsSendResponse.failure(e.getMessage(), "EXCEPTION", primary.getProviderName());
        }
    }

    /**
     * Send SMS using fallback provider.
     */
    private SmsSendResponse sendWithFallback(SmsMessage smsMessage) {
        SmsProvider fallback = getFallbackProvider();

        if (fallback == null || !fallback.isAvailable()) {
            log.error("Fallback SMS provider is not available");
            return SmsSendResponse.failure(
                    "Fallback SMS provider not available",
                    "NO_FALLBACK",
                    smsProperties.getFallbackProvider() != null ? smsProperties.getFallbackProvider().name() : "NONE"
            );
        }

        try {
            log.info("Using fallback SMS provider: {}", fallback.getProviderName());
            return fallback.sendSms(smsMessage);
        } catch (Exception e) {
            log.error("Fallback SMS provider also failed: {}", e.getMessage());
            return SmsSendResponse.failure(e.getMessage(), "FALLBACK_FAILED", fallback.getProviderName());
        }
    }

    /**
     * Check if any SMS provider is available.
     */
    public boolean isAnyProviderAvailable() {
        if (!smsProperties.isEnabled()) {
            return false;
        }

        SmsProvider primary = getPrimaryProvider();
        if (primary != null && primary.isAvailable()) {
            return true;
        }

        if (smsProperties.isFallbackEnabled()) {
            SmsProvider fallback = getFallbackProvider();
            return fallback != null && fallback.isAvailable();
        }

        return false;
    }

    /**
     * Get the name of the currently active provider.
     */
    public String getActiveProviderName() {
        SmsProvider primary = getPrimaryProvider();
        if (primary != null && primary.isAvailable()) {
            return primary.getProviderName();
        }

        if (smsProperties.isFallbackEnabled()) {
            SmsProvider fallback = getFallbackProvider();
            if (fallback != null && fallback.isAvailable()) {
                return fallback.getProviderName() + " (fallback)";
            }
        }

        return "NONE";
    }
}
