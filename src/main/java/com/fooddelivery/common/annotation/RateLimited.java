package com.fooddelivery.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply rate limiting to controller methods.
 * Uses Redis-backed token bucket algorithm.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    /**
     * Number of requests allowed per minute.
     * Default is 60 requests per minute.
     */
    int requestsPerMinute() default 60;

    /**
     * Maximum burst capacity.
     * Default is 10.
     */
    int burstCapacity() default 10;

    /**
     * Key type for rate limiting.
     * IP - Rate limit by IP address
     * USER - Rate limit by authenticated user
     * ENDPOINT - Rate limit by endpoint + identifier
     */
    KeyType keyType() default KeyType.IP;

    enum KeyType {
        IP,
        USER,
        ENDPOINT
    }
}
