package com.fooddelivery.analytics.cx.config;

import com.fooddelivery.analytics.cx.service.CustomerExperienceAnalyticsServiceImpl;
import org.springframework.cache.CacheManager;
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
 * Redis cache configuration for Customer Experience Analytics.
 *
 * TTL Configuration:
 * - NPS metrics: 10 minutes (data changes less frequently)
 * - Restaurant ratings: 5 minutes (moderate change frequency)
 * - Courier ratings: 5 minutes (moderate change frequency)
 * - App store ratings: 5 minutes (moderate change frequency)
 * - Support tickets: 1 minute (data changes frequently)
 * - CX Summary: 5 minutes (aggregate of all metrics)
 */
@Configuration
public class CxCacheConfig {

    // TTL constants in minutes
    private static final long NPS_TTL_MINUTES = 10;
    private static final long RATINGS_TTL_MINUTES = 5;
    private static final long SUPPORT_TTL_MINUTES = 1;
    private static final long SUMMARY_TTL_MINUTES = 5;

    /**
     * Configure CX-specific cache manager with custom TTLs.
     */
    @Bean(name = "cxCacheManager")
    public CacheManager cxCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = createDefaultConfig();

        // Configure cache-specific TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // NPS cache - 10 min TTL
        cacheConfigurations.put(
                CustomerExperienceAnalyticsServiceImpl.NPS_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(NPS_TTL_MINUTES))
        );

        // Restaurant ratings cache - 5 min TTL
        cacheConfigurations.put(
                CustomerExperienceAnalyticsServiceImpl.RESTAURANT_RATING_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(RATINGS_TTL_MINUTES))
        );

        // Courier ratings cache - 5 min TTL
        cacheConfigurations.put(
                CustomerExperienceAnalyticsServiceImpl.COURIER_RATING_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(RATINGS_TTL_MINUTES))
        );

        // App store ratings cache - 5 min TTL
        cacheConfigurations.put(
                CustomerExperienceAnalyticsServiceImpl.APP_STORE_RATING_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(RATINGS_TTL_MINUTES))
        );

        // Support tickets cache - 1 min TTL
        cacheConfigurations.put(
                CustomerExperienceAnalyticsServiceImpl.SUPPORT_TICKET_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(SUPPORT_TTL_MINUTES))
        );

        // CX Summary cache - 5 min TTL
        cacheConfigurations.put(
                CustomerExperienceAnalyticsServiceImpl.CX_SUMMARY_CACHE,
                defaultConfig.entryTtl(Duration.ofMinutes(SUMMARY_TTL_MINUTES))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * Create default cache configuration.
     */
    private RedisCacheConfiguration createDefaultConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues()
                .prefixCacheNameWith("food-delivery:cx:");
    }
}
