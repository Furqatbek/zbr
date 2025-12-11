package com.fooddelivery.analytics.financial.config;

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
 * Redis cache configuration for financial analytics.
 *
 * Cache TTL Configuration:
 * - GMV metrics: 60 seconds (real-time critical)
 * - Commission metrics: 300 seconds (5 minutes)
 * - Delivery fee metrics: 120 seconds (2 minutes)
 * - Promotion metrics: 300 seconds (5 minutes)
 * - Restaurant payout metrics: 300 seconds (5 minutes)
 * - Courier payout metrics: 300 seconds (5 minutes)
 * - Contribution margin: 60 seconds (real-time critical)
 */
@Configuration
@EnableCaching
public class FinancialCacheConfig {

    public static final String CACHE_GMV_METRICS = "gmv-metrics";
    public static final String CACHE_COMMISSION_METRICS = "commission-metrics";
    public static final String CACHE_DELIVERY_FEE_METRICS = "delivery-fee-metrics";
    public static final String CACHE_PROMOTION_METRICS = "promotion-metrics";
    public static final String CACHE_RESTAURANT_PAYOUT_METRICS = "restaurant-payout-metrics";
    public static final String CACHE_COURIER_PAYOUT_METRICS = "courier-payout-metrics";
    public static final String CACHE_CONTRIBUTION_MARGIN = "contribution-margin";
    public static final String CACHE_FINANCIAL_SUMMARY = "financial-summary";

    @Bean
    public CacheManager financialCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // GMV metrics - 60 seconds TTL (real-time critical)
        cacheConfigurations.put(CACHE_GMV_METRICS,
                defaultConfig.entryTtl(Duration.ofSeconds(60)));

        // Commission metrics - 300 seconds TTL
        cacheConfigurations.put(CACHE_COMMISSION_METRICS,
                defaultConfig.entryTtl(Duration.ofSeconds(300)));

        // Delivery fee metrics - 120 seconds TTL
        cacheConfigurations.put(CACHE_DELIVERY_FEE_METRICS,
                defaultConfig.entryTtl(Duration.ofSeconds(120)));

        // Promotion metrics - 300 seconds TTL
        cacheConfigurations.put(CACHE_PROMOTION_METRICS,
                defaultConfig.entryTtl(Duration.ofSeconds(300)));

        // Restaurant payout metrics - 300 seconds TTL
        cacheConfigurations.put(CACHE_RESTAURANT_PAYOUT_METRICS,
                defaultConfig.entryTtl(Duration.ofSeconds(300)));

        // Courier payout metrics - 300 seconds TTL
        cacheConfigurations.put(CACHE_COURIER_PAYOUT_METRICS,
                defaultConfig.entryTtl(Duration.ofSeconds(300)));

        // Contribution margin - 60 seconds TTL (real-time critical)
        cacheConfigurations.put(CACHE_CONTRIBUTION_MARGIN,
                defaultConfig.entryTtl(Duration.ofSeconds(60)));

        // Financial summary - 60 seconds TTL
        cacheConfigurations.put(CACHE_FINANCIAL_SUMMARY,
                defaultConfig.entryTtl(Duration.ofSeconds(60)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
