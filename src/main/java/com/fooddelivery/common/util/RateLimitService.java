package com.fooddelivery.common.util;

import com.fooddelivery.common.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis-backed rate limiting service using token bucket algorithm.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${app.rate-limit.burst-capacity:10}")
    private int burstCapacity;

    // In-memory bucket cache for performance
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /**
     * Check if the request is within rate limits.
     *
     * @param key Unique identifier (e.g., user ID, IP address)
     * @return true if request is allowed
     */
    public boolean tryConsume(String key) {
        if (!rateLimitEnabled) {
            return true;
        }

        Bucket bucket = getBucket(key);
        return bucket.tryConsume(1);
    }

    /**
     * Check and throw exception if rate limit exceeded.
     *
     * @param key Unique identifier (e.g., user ID, IP address)
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    public void checkRateLimit(String key) {
        if (!rateLimitEnabled) {
            return;
        }

        if (!tryConsume(key)) {
            log.warn("Rate limit exceeded for key: {}", key);
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Please try again later.",
                    60
            );
        }
    }

    /**
     * Check rate limit with custom parameters.
     *
     * @param key              Unique identifier
     * @param tokensPerMinute  Allowed requests per minute
     * @param burstSize        Maximum burst capacity
     */
    public void checkRateLimit(String key, int tokensPerMinute, int burstSize) {
        if (!rateLimitEnabled) {
            return;
        }

        Bucket bucket = getCustomBucket(key, tokensPerMinute, burstSize);
        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for key: {} (custom: {}/min)", key, tokensPerMinute);
            throw new RateLimitExceededException(
                    "Rate limit exceeded. Please try again later.",
                    60
            );
        }
    }

    /**
     * Get remaining tokens for a key.
     *
     * @param key Unique identifier
     * @return Number of remaining tokens
     */
    public long getRemainingTokens(String key) {
        Bucket bucket = getBucket(key);
        return bucket.getAvailableTokens();
    }

    /**
     * Reset rate limit for a key.
     *
     * @param key Unique identifier
     */
    public void resetRateLimit(String key) {
        String cacheKey = RATE_LIMIT_PREFIX + key;
        bucketCache.remove(cacheKey);
        redisTemplate.delete(cacheKey);
    }

    private Bucket getBucket(String key) {
        String cacheKey = RATE_LIMIT_PREFIX + key;
        return bucketCache.computeIfAbsent(cacheKey, k -> createBucket(requestsPerMinute, burstCapacity));
    }

    private Bucket getCustomBucket(String key, int tokensPerMinute, int burstSize) {
        String cacheKey = RATE_LIMIT_PREFIX + key + ":" + tokensPerMinute;
        return bucketCache.computeIfAbsent(cacheKey, k -> createBucket(tokensPerMinute, burstSize));
    }

    private Bucket createBucket(int tokensPerMinute, int burstSize) {
        // Gradual refill over the minute
        Bandwidth limit = Bandwidth.classic(
                burstSize + tokensPerMinute,
                Refill.greedy(tokensPerMinute, Duration.ofMinutes(1))
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create rate limit key from IP address.
     */
    public static String ipKey(String ipAddress) {
        return "ip:" + ipAddress;
    }

    /**
     * Create rate limit key from user ID.
     */
    public static String userKey(Long userId) {
        return "user:" + userId;
    }

    /**
     * Create rate limit key from endpoint.
     */
    public static String endpointKey(String endpoint, String identifier) {
        return "endpoint:" + endpoint + ":" + identifier;
    }
}
