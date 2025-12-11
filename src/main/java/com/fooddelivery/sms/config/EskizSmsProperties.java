package com.fooddelivery.sms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Eskiz SMS gateway.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.sms.eskiz")
public class EskizSmsProperties {

    /**
     * Whether SMS sending is enabled.
     */
    private boolean enabled = true;

    /**
     * Base URL for Eskiz API.
     */
    private String baseUrl = "https://notify.eskiz.uz/api";

    /**
     * Email for authentication.
     */
    private String email;

    /**
     * Password for authentication.
     */
    private String password;

    /**
     * Sender name (alpha-name) displayed to recipients.
     */
    private String from = "4546";

    /**
     * Token time-to-live in seconds (default 29 days).
     */
    private long tokenTtlSeconds = 2505600;

    /**
     * Connection timeout in milliseconds.
     */
    private int connectTimeout = 5000;

    /**
     * Read timeout in milliseconds.
     */
    private int readTimeout = 30000;

    /**
     * Maximum retry attempts for failed requests.
     */
    private int maxRetries = 3;

    /**
     * Callback URL for delivery reports.
     */
    private String callbackUrl;
}
