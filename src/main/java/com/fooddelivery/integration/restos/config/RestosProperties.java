package com.fooddelivery.integration.restos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.integration.restos")
@Data
public class RestosProperties {
    /** Master switch. When false, all import/preview calls are rejected. */
    private boolean enabled = true;
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 10000;

    /**
     * Optional allowlist of permitted hostnames for the external Restos base URL.
     * When empty, any PUBLIC host is allowed (private/loopback/link-local IPs are
     * always blocked by UrlSafetyValidator regardless of this list). Set this in
     * production to lock imports down to known Restos deployments.
     */
    private List<String> allowedHosts = new ArrayList<>();
}
