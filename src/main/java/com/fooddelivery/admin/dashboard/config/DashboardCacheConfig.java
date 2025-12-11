package com.fooddelivery.admin.dashboard.config;

import com.fooddelivery.admin.dashboard.util.DashboardConstants;
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
 * Redis cache configuration for admin dashboard.
 * Configures cache TTLs for different dashboard components.
 */
@Configuration
@EnableCaching
public class DashboardCacheConfig {

    /**
     * Configure Redis cache manager with component-specific TTLs.
     */
    @Bean
    public CacheManager dashboardCacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Component-specific cache configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Overview cache - 15 seconds TTL (frequently changing)
        cacheConfigurations.put(DashboardConstants.CACHE_OVERVIEW,
                defaultConfig.entryTtl(Duration.ofSeconds(DashboardConstants.CACHE_TTL_OVERVIEW)));

        // Active orders cache - 5 seconds TTL (real-time critical)
        cacheConfigurations.put(DashboardConstants.CACHE_ACTIVE_ORDERS,
                defaultConfig.entryTtl(Duration.ofSeconds(DashboardConstants.CACHE_TTL_ACTIVE_ORDERS)));

        // Stuck orders cache - 10 seconds TTL (high priority)
        cacheConfigurations.put(DashboardConstants.CACHE_STUCK_ORDERS,
                defaultConfig.entryTtl(Duration.ofSeconds(DashboardConstants.CACHE_TTL_STUCK_ORDERS)));

        // Restaurant metrics cache - 30 seconds TTL
        cacheConfigurations.put(DashboardConstants.CACHE_RESTAURANT_METRICS,
                defaultConfig.entryTtl(Duration.ofSeconds(DashboardConstants.CACHE_TTL_RESTAURANT)));

        // Courier metrics cache - 15 seconds TTL (location tracking)
        cacheConfigurations.put(DashboardConstants.CACHE_COURIER_METRICS,
                defaultConfig.entryTtl(Duration.ofSeconds(DashboardConstants.CACHE_TTL_COURIER)));

        // Finance metrics cache - 30 seconds TTL
        cacheConfigurations.put(DashboardConstants.CACHE_FINANCE_METRICS,
                defaultConfig.entryTtl(Duration.ofSeconds(DashboardConstants.CACHE_TTL_FINANCE)));

        // Support metrics cache - 30 seconds TTL
        cacheConfigurations.put(DashboardConstants.CACHE_SUPPORT_METRICS,
                defaultConfig.entryTtl(Duration.ofSeconds(DashboardConstants.CACHE_TTL_SUPPORT)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
