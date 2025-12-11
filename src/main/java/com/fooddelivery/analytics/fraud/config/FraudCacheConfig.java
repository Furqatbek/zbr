package com.fooddelivery.analytics.fraud.config;

import com.fooddelivery.analytics.fraud.util.FraudConstants;
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
 * Redis cache configuration for fraud analytics module.
 * Configures different TTL values for different fraud metric categories:
 * - Fraud metrics: 60 seconds
 * - Security metrics: 30 seconds
 * - Referral metrics: 5 minutes
 * - Fraud summary: 2 minutes
 */
@Configuration
@EnableCaching
public class FraudCacheConfig {

    @Bean
    public CacheManager fraudCacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(FraudConstants.CACHE_TTL_FRAUD_SECONDS))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Cache-specific configurations with different TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Fraud metrics cache - 60 seconds TTL
        cacheConfigurations.put(FraudConstants.CACHE_NAME_FRAUD_METRICS,
                defaultConfig.entryTtl(Duration.ofSeconds(FraudConstants.CACHE_TTL_FRAUD_SECONDS)));

        // Security metrics cache - 30 seconds TTL (more real-time)
        cacheConfigurations.put(FraudConstants.CACHE_NAME_SECURITY_METRICS,
                defaultConfig.entryTtl(Duration.ofSeconds(FraudConstants.CACHE_TTL_SECURITY_SECONDS)));

        // Referral metrics cache - 5 minutes TTL (less volatile)
        cacheConfigurations.put(FraudConstants.CACHE_NAME_REFERRAL_METRICS,
                defaultConfig.entryTtl(Duration.ofSeconds(FraudConstants.CACHE_TTL_REFERRAL_SECONDS)));

        // Fraud summary cache - 2 minutes TTL
        cacheConfigurations.put(FraudConstants.CACHE_NAME_FRAUD_SUMMARY,
                defaultConfig.entryTtl(Duration.ofSeconds(FraudConstants.CACHE_TTL_SUMMARY_SECONDS)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
