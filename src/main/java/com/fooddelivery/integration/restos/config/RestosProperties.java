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

    /**
     * Ceiling on how much of a restaurant's externally-sourced menu one sync may
     * deactivate, as a fraction of what is currently live.
     *
     * <p>A sync retires whatever the upstream snapshot no longer contains, which
     * is correct when the snapshot is correct — and catastrophic when Restos has
     * a partial outage and answers with three dishes instead of two hundred. The
     * cost of the two mistakes is not symmetric: a stale item left on the menu
     * for another day is a nuisance, an emptied menu is a restaurant taking no
     * orders. Above this fraction the sync reports what it would have done and
     * changes nothing.
     *
     * <p>Only applies past {@code deactivationFloor} items, so a small menu
     * losing one dish is not mistaken for a wipe.
     */
    private double maxDeactivationRatio = 0.30;

    /**
     * Number of deactivations always permitted regardless of the ratio above.
     * Without it, a five-item menu could never drop a dish.
     */
    private int deactivationFloor = 5;
}
