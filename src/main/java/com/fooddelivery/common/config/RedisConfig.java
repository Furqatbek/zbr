package com.fooddelivery.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis configuration for caching and distributed operations.
 */
@Configuration
@EnableCaching
@Slf4j
public class RedisConfig implements CachingConfigurer {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Configure serializers
        ObjectMapper objectMapper = createObjectMapper();
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = createObjectMapper();
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        // Configure specific cache TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Restaurant cache - 5 minutes
        cacheConfigurations.put("restaurants", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Menu cache - 5 minutes
        cacheConfigurations.put("menus", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Menu items cache - 5 minutes
        cacheConfigurations.put("menuItems", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // User cache - 15 minutes
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Order cache - 1 minute (frequently updated)
        cacheConfigurations.put("orders", defaultConfig.entryTtl(Duration.ofMinutes(1)));

        // Courier location cache - 30 seconds
        cacheConfigurations.put("courierLocations", defaultConfig.entryTtl(Duration.ofSeconds(30)));

        // Rate limit cache - 1 minute
        cacheConfigurations.put("rateLimit", defaultConfig.entryTtl(Duration.ofMinutes(1)));

        // Delivery fee settings cache - 10 minutes
        cacheConfigurations.put("deliveryFeeSettings", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new RedisCacheErrorHandler();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return objectMapper;
    }

    /**
     * Handles Redis cache errors gracefully. On deserialization failures (e.g., stale
     * entries from a previous serializer config), the error is logged and the cache
     * entry is evicted so it gets re-populated with the correct format on next access.
     */
    static class RedisCacheErrorHandler implements CacheErrorHandler {

        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            log.warn("Cache get error on cache '{}', key '{}': {}. Evicting stale entry.",
                    cache.getName(), key, exception.getMessage());
            try {
                cache.evict(key);
            } catch (RuntimeException evictEx) {
                log.warn("Failed to evict stale cache entry '{}' from '{}': {}",
                        key, cache.getName(), evictEx.getMessage());
            }
        }

        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            log.warn("Cache put error on cache '{}', key '{}': {}",
                    cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            log.warn("Cache evict error on cache '{}', key '{}': {}",
                    cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            log.warn("Cache clear error on cache '{}': {}",
                    cache.getName(), exception.getMessage());
        }
    }
}
