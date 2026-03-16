package com.fooddelivery.sms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Main SMS configuration properties.
 * Controls which SMS provider to use.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.sms")
public class SmsProperties {

    /**
     * Whether SMS sending is globally enabled.
     */
    private boolean enabled = true;

    /**
     * Active SMS provider to use.
     * Options: ESKIZ, DEVSMS
     */
    private SmsProviderType provider = SmsProviderType.ESKIZ;

    /**
     * Fallback provider if primary fails.
     * Set to null to disable fallback.
     */
    private SmsProviderType fallbackProvider;

    /**
     * Whether to use fallback provider on failure.
     */
    private boolean fallbackEnabled = false;

    /**
     * SMS provider types.
     */
    public enum SmsProviderType {
        ESKIZ,
        DEVSMS
    }
}
