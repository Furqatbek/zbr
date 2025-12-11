package com.fooddelivery.analytics.config;

import com.fooddelivery.analytics.service.impl.AnalyticsServiceImpl;
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
 * Redis cache configuration for analytics metrics.
 *
 * <p>TTL Configuration:</p>
 * <ul>
 *   <li>DAU/WAU/MAU: 5 minutes - Relatively stable, refreshed periodically</li>
 *   <li>Order volume: 1 minute - More dynamic, needs frequent updates</li>
 *   <li>Conversion funnel: 30 seconds - Real-time conversion tracking</li>
 *   <li>AOV: 1 minute - Revenue metrics with moderate refresh</li>
 *   <li>Activation: 5 minutes - User activation doesn't change rapidly</li>
 *   <li>Churn: 10 minutes - Long-term metric, less frequent updates</li>
 * </ul>
 */
@Configuration
public class AnalyticsCacheConfig {

    @Bean
    public CacheManager analyticsCacheManager(RedisConnectionFactory connectionFactory) {
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

        // DAU/WAU/MAU: 5 minutes TTL
        cacheConfigurations.put(AnalyticsServiceImpl.CACHE_DAU_WAU_MAU,
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Order volume: 1 minute TTL
        cacheConfigurations.put(AnalyticsServiceImpl.CACHE_ORDER_VOLUME,
                defaultConfig.entryTtl(Duration.ofMinutes(1)));

        // Conversion funnel: 30 seconds TTL
        cacheConfigurations.put(AnalyticsServiceImpl.CACHE_CONVERSION,
                defaultConfig.entryTtl(Duration.ofSeconds(30)));

        // AOV: 1 minute TTL
        cacheConfigurations.put(AnalyticsServiceImpl.CACHE_AOV,
                defaultConfig.entryTtl(Duration.ofMinutes(1)));

        // Activation metrics: 5 minutes TTL
        cacheConfigurations.put(AnalyticsServiceImpl.CACHE_ACTIVATION,
                defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Churn metrics: 10 minutes TTL
        cacheConfigurations.put(AnalyticsServiceImpl.CACHE_CHURN,
                defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
