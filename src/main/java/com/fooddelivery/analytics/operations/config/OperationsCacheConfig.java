package com.fooddelivery.analytics.operations.config;

import com.fooddelivery.analytics.operations.service.impl.OperationsAnalyticsServiceImpl;
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
 * Redis cache configuration for operations analytics metrics.
 *
 * <p>TTL Configuration:</p>
 * <ul>
 *   <li>Order Fulfillment: 60 seconds - Real-time order tracking</li>
 *   <li>Restaurant Performance: 5 minutes - Moderate refresh for dashboard</li>
 *   <li>Courier Performance: 1 minute - Needs frequent updates</li>
 *   <li>Delivery Success Rate: 2 minutes - Aggregate metric</li>
 *   <li>ETA Accuracy: 30 seconds - Real-time accuracy tracking</li>
 *   <li>Operations Summary: 2 minutes - Dashboard overview</li>
 * </ul>
 */
@Configuration
public class OperationsCacheConfig {

    @Bean
    public CacheManager operationsCacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Custom TTL per cache
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Order Fulfillment: 60 seconds TTL - Real-time tracking
        cacheConfigurations.put(OperationsAnalyticsServiceImpl.CACHE_ORDER_FULFILLMENT,
                defaultConfig.entryTtl(Duration.ofSeconds(60)));

        // Restaurant Performance: 5 minutes TTL - Dashboard metrics
        cacheConfigurations.put(OperationsAnalyticsServiceImpl.CACHE_RESTAURANT_PERFORMANCE,
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Courier Performance: 1 minute TTL - Frequent updates needed
        cacheConfigurations.put(OperationsAnalyticsServiceImpl.CACHE_COURIER_PERFORMANCE,
                defaultConfig.entryTtl(Duration.ofMinutes(1)));

        // Delivery Success Rate: 2 minutes TTL - Aggregate metric
        cacheConfigurations.put(OperationsAnalyticsServiceImpl.CACHE_DELIVERY_SUCCESS,
                defaultConfig.entryTtl(Duration.ofMinutes(2)));

        // ETA Accuracy: 30 seconds TTL - Real-time accuracy
        cacheConfigurations.put(OperationsAnalyticsServiceImpl.CACHE_ETA_ACCURACY,
                defaultConfig.entryTtl(Duration.ofSeconds(30)));

        // Operations Summary: 2 minutes TTL - Dashboard overview
        cacheConfigurations.put(OperationsAnalyticsServiceImpl.CACHE_OPERATIONS_SUMMARY,
                defaultConfig.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
