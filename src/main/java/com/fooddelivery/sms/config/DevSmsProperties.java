package com.fooddelivery.sms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for DevSMS gateway.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.sms.devsms")
public class DevSmsProperties {

    /**
     * Whether DevSMS is enabled.
     */
    private boolean enabled = false;

    /**
     * Base URL for DevSMS API.
     */
    private String baseUrl = "https://devsms.uz/api";

    /**
     * Bearer token for authentication.
     */
    private String token;

    /**
     * Sender ID displayed to recipients.
     */
    private String from = "4546";

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
