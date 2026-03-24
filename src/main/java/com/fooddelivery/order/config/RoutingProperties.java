package com.fooddelivery.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the route distance calculation service (OSRM).
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.delivery.routing")
public class RoutingProperties {

    /**
     * Whether route-based distance calculation is enabled.
     * When disabled, falls back to Haversine (straight-line) calculation.
     */
    private boolean enabled = false;

    /**
     * Base URL of the OSRM routing server.
     * Default points to the public OSRM demo server.
     * For production, deploy your own OSRM instance.
     */
    private String osrmBaseUrl = "https://router.project-osrm.org";

    /**
     * Connection timeout in milliseconds.
     */
    private int connectTimeout = 3000;

    /**
     * Read timeout in milliseconds.
     */
    private int readTimeout = 5000;
}
