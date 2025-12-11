package com.fooddelivery.analytics.technical.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration for technical metrics.
 * Defines cache names and TTLs as per requirements:
 * - API: 30s
 * - Backend: 10s
 * - Database: 60s
 * - WebSocket: 5s
 * - Storage: 60s
 */
@Configuration
@EnableCaching
public class TechnicalMetricsCacheConfig {

    // Cache names
    public static final String CACHE_API_METRICS = "technicalApiMetrics";
    public static final String CACHE_BACKEND_METRICS = "technicalBackendMetrics";
    public static final String CACHE_DB_METRICS = "technicalDbMetrics";
    public static final String CACHE_WS_METRICS = "technicalWsMetrics";
    public static final String CACHE_STORAGE_METRICS = "technicalStorageMetrics";
    public static final String CACHE_SUMMARY = "technicalSummary";

    // TTLs as per requirements
    private static final Duration API_TTL = Duration.ofSeconds(30);
    private static final Duration BACKEND_TTL = Duration.ofSeconds(10);
    private static final Duration DB_TTL = Duration.ofSeconds(60);
    private static final Duration WS_TTL = Duration.ofSeconds(5);
    private static final Duration STORAGE_TTL = Duration.ofSeconds(60);
    private static final Duration SUMMARY_TTL = Duration.ofSeconds(30);

    @Bean
    public CacheManager technicalMetricsCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // API metrics - 30 second TTL
        cacheConfigurations.put(CACHE_API_METRICS, defaultConfig.entryTtl(API_TTL));

        // Backend metrics - 10 second TTL (short for real-time resource monitoring)
        cacheConfigurations.put(CACHE_BACKEND_METRICS, defaultConfig.entryTtl(BACKEND_TTL));

        // Database metrics - 60 second TTL (queries/stats don't change as frequently)
        cacheConfigurations.put(CACHE_DB_METRICS, defaultConfig.entryTtl(DB_TTL));

        // WebSocket metrics - 5 second TTL (very short for real-time connection status)
        cacheConfigurations.put(CACHE_WS_METRICS, defaultConfig.entryTtl(WS_TTL));

        // Storage metrics - 60 second TTL (file stats relatively stable)
        cacheConfigurations.put(CACHE_STORAGE_METRICS, defaultConfig.entryTtl(STORAGE_TTL));

        // Summary - 30 second TTL
        cacheConfigurations.put(CACHE_SUMMARY, defaultConfig.entryTtl(SUMMARY_TTL));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
