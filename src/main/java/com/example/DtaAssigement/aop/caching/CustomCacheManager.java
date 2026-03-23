package com.example.DtaAssigement.aop.caching;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis-based cache manager
 * Thread-safe implementation using RedisTemplate
 * Replaces ConcurrentHashMap with distributed Redis cache
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CustomCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;

    // Statistics (stored in Redis)
    private static final String STATS_HIT_KEY = "cache:stats:hits";
    private static final String STATS_MISS_KEY = "cache:stats:misses";
    private static final String CACHE_KEY_PREFIX = "cafe::";

    /**
     * Get value from cache
     * Returns null if Redis is unavailable
     */
    public Object get(String key) {
        if (redisTemplate == null) {
            log.debug("Redis unavailable - Cache GET skipped: {}", key);
            return null;
        }

        try {
            String redisKey = CACHE_KEY_PREFIX + key;
            Object value = redisTemplate.opsForValue().get(redisKey);

            if (value == null) {
                incrementMiss();
                log.debug("Cache MISS: {}", key);
                return null;
            }

            incrementHit();
            log.debug("Cache HIT: {}", key);
            return value;
        } catch (Exception e) {
            log.warn("Redis error during GET operation for key '{}': {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Put value into cache with TTL
     * Silently skips if Redis is unavailable
     */
    public void put(String key, Object value, int ttlSeconds) {
        if (redisTemplate == null) {
            log.debug("Redis unavailable - Cache PUT skipped: {}", key);
            return;
        }

        try {
            String redisKey = CACHE_KEY_PREFIX + key;
            redisTemplate.opsForValue().set(redisKey, value, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Cache PUT: {} (TTL: {}s)", key, ttlSeconds);
        } catch (Exception e) {
            log.warn("Redis error during PUT operation for key '{}': {}", key, e.getMessage());
        }
    }

    /**
     * Evict specific key from cache
     * Silently skips if Redis is unavailable
     */
    public void evict(String key) {
        if (redisTemplate == null) {
            log.debug("Redis unavailable - Cache EVICT skipped: {}", key);
            return;
        }

        try {
            String redisKey = CACHE_KEY_PREFIX + key;
            redisTemplate.delete(redisKey);
            log.debug("Cache EVICT: {}", key);
        } catch (Exception e) {
            log.warn("Redis error during EVICT operation for key '{}': {}", key, e.getMessage());
        }
    }

    /**
     * Evict multiple keys
     */
    public void evictAll(String... keys) {
        for (String key : keys) {
            evict(key);
        }
    }

    /**
     * Clear entire cache
     * Silently skips if Redis is unavailable
     */
    public void clear() {
        if (redisTemplate == null) {
            log.debug("Redis unavailable - Cache CLEAR skipped");
            return;
        }

        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                log.info("Cache CLEARED: {} entries removed", deleted);
            }
        } catch (Exception e) {
            log.warn("Redis error during CLEAR operation: {}", e.getMessage());
        }
    }

    /**
     * Get cache statistics
     * Returns empty stats if Redis is unavailable
     */
    public CacheStats getStats() {
        if (redisTemplate == null) {
            log.debug("Redis unavailable - returning empty stats");
            return new CacheStats(0, 0L, 0L, 0.0);
        }

        try {
            Long hits = getStatValue(STATS_HIT_KEY);
            Long misses = getStatValue(STATS_MISS_KEY);
            long total = hits + misses;
            double hitRate = total > 0 ? (double) hits / total * 100 : 0;

            // Get cache size
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            int size = keys != null ? keys.size() : 0;

            return new CacheStats(size, hits, misses, hitRate);
        } catch (Exception e) {
            log.warn("Redis error during GET_STATS operation: {}", e.getMessage());
            return new CacheStats(0, 0L, 0L, 0.0);
        }
    }

    /**
     * Reset statistics
     * Silently skips if Redis is unavailable
     */
    public void resetStats() {
        if (redisTemplate == null) {
            log.debug("Redis unavailable - Stats RESET skipped");
            return;
        }

        try {
            redisTemplate.delete(STATS_HIT_KEY);
            redisTemplate.delete(STATS_MISS_KEY);
            log.info("Cache statistics reset");
        } catch (Exception e) {
            log.warn("Redis error during RESET_STATS operation: {}", e.getMessage());
        }
    }

    /**
     * Clean up expired entries
     * Redis handles TTL automatically, so this is a no-op
     */
    public void cleanupExpired() {
        log.debug("Redis handles TTL expiration automatically");
    }

    // Helper methods for statistics
    private void incrementHit() {
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().increment(STATS_HIT_KEY);
            } catch (Exception e) {
                log.debug("Redis error incrementing hit counter: {}", e.getMessage());
            }
        }
    }

    private void incrementMiss() {
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().increment(STATS_MISS_KEY);
            } catch (Exception e) {
                log.debug("Redis error incrementing miss counter: {}", e.getMessage());
            }
        }
    }

    private Long getStatValue(String key) {
        if (redisTemplate == null) {
            return 0L;
        }

        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value.toString()) : 0L;
        } catch (Exception e) {
            log.debug("Redis error getting stat value for '{}': {}", key, e.getMessage());
            return 0L;
        }
    }

    /**
     * Cache statistics record
     */
    public record CacheStats(
            int size,
            long hits,
            long misses,
            double hitRate) {
    }
}
