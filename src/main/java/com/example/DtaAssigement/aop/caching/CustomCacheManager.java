package com.example.DtaAssigement.aop.caching;

import com.example.DtaAssigement.config.RedisConfig;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Redis-based cache manager
 * Thread-safe implementation using RedisTemplate
 * Replaces ConcurrentHashMap with distributed Redis cache
 * Handles Redis connection failures gracefully with automatic retry
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CustomCacheManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;

    // Statistics (stored in Redis)
    private static final String STATS_HIT_KEY = "cache:stats:hits";
    private static final String STATS_MISS_KEY = "cache:stats:misses";
    private static final String CACHE_KEY_PREFIX = "cafe::";

    @Getter
    private final AtomicReference<ConnectionState> connectionState =
            new AtomicReference<>(ConnectionState.UNKNOWN);

    private long lastSuccessfulOperation = 0;
    private static final long RECONNECT_CHECK_INTERVAL = 30000; // 30 seconds

    public enum ConnectionState {
        UNKNOWN,
        CONNECTED,
        DISCONNECTED,
        ERROR
    }

    @PostConstruct
    public void init() {
        updateConnectionState();
    }

    /**
     * Periodically check and update connection state
     */
    @Scheduled(fixedDelay = RECONNECT_CHECK_INTERVAL, initialDelay = RECONNECT_CHECK_INTERVAL)
    public void checkConnectionState() {
        updateConnectionState();
    }

    /**
     * Update the connection state based on Redis availability
     */
    private void updateConnectionState() {
        if (redisTemplate == null) {
            setConnectionState(ConnectionState.DISCONNECTED);
            return;
        }

        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            if (connectionState.get() != ConnectionState.CONNECTED) {
                log.info("✅ Redis connection restored. Cache operations resumed.");
            }
            setConnectionState(ConnectionState.CONNECTED);
        } catch (Exception e) {
            if (connectionState.get() != ConnectionState.DISCONNECTED) {
                log.warn("⚠️ Redis connection lost: {}. Cache operations will be skipped until reconnection.",
                        e.getMessage());
            }
            setConnectionState(ConnectionState.DISCONNECTED);
        }
    }

    private void setConnectionState(ConnectionState newState) {
        ConnectionState oldState = connectionState.getAndSet(newState);
        if (oldState != newState) {
            log.debug("Redis connection state changed: {} -> {}", oldState, newState);
        }
    }

    /**
     * Check if cache operations can be performed
     */
    public boolean isAvailable() {
        return redisTemplate != null && connectionState.get() == ConnectionState.CONNECTED;
    }

    /**
     * Get value from cache with retry logic
     * Returns null if Redis is unavailable
     */
    public Object get(String key) {
        if (redisTemplate == null || !isAvailable()) {
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
            lastSuccessfulOperation = System.currentTimeMillis();
            log.debug("Cache HIT: {}", key);
            return value;
        } catch (RedisConnectionFailureException e) {
            handleConnectionFailure(e);
            return null;
        } catch (Exception e) {
            log.warn("Redis error during GET operation for key '{}': {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Put value into cache with retry logic
     * Silently skips if Redis is unavailable
     */
    public void put(String key, Object value, int ttlSeconds) {
        if (redisTemplate == null || !isAvailable()) {
            log.debug("Redis unavailable - Cache PUT skipped: {}", key);
            return;
        }

        try {
            String redisKey = CACHE_KEY_PREFIX + key;
            redisTemplate.opsForValue().set(redisKey, value, ttlSeconds, TimeUnit.SECONDS);
            lastSuccessfulOperation = System.currentTimeMillis();
            log.debug("Cache PUT: {} (TTL: {}s)", key, ttlSeconds);
        } catch (RedisConnectionFailureException e) {
            handleConnectionFailure(e);
        } catch (Exception e) {
            log.warn("Redis error during PUT operation for key '{}': {}", key, e.getMessage());
        }
    }

    /**
     * Evict specific key from cache
     * Silently skips if Redis is unavailable
     */
    public void evict(String key) {
        if (redisTemplate == null || !isAvailable()) {
            log.debug("Redis unavailable - Cache EVICT skipped: {}", key);
            return;
        }

        try {
            String redisKey = CACHE_KEY_PREFIX + key;
            redisTemplate.delete(redisKey);
            lastSuccessfulOperation = System.currentTimeMillis();
            log.debug("Cache EVICT: {}", key);
        } catch (RedisConnectionFailureException e) {
            handleConnectionFailure(e);
        } catch (Exception e) {
            log.warn("Redis error during EVICT operation for key '{}': {}", key, e.getMessage());
        }
    }

    /**
     * Handle Redis connection failure
     */
    private void handleConnectionFailure(Exception e) {
        setConnectionState(ConnectionState.DISCONNECTED);
        log.warn("⚠️ Redis connection failed: {}. Will retry on next operation.", e.getMessage());
    }

    /**
     * Force a connection check
     */
    public void refreshConnection() {
        log.info("🔄 Refreshing Redis connection...");
        redisConfig.refreshConnection();
        updateConnectionState();
    }

    /**
     * Check if Redis should be available based on last operation time
     */
    public boolean shouldRetryConnection() {
        return System.currentTimeMillis() - lastSuccessfulOperation > RECONNECT_CHECK_INTERVAL;
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
        if (redisTemplate == null || !isAvailable()) {
            log.debug("Redis unavailable - Cache CLEAR skipped");
            return;
        }

        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                lastSuccessfulOperation = System.currentTimeMillis();
                log.info("Cache CLEARED: {} entries removed", deleted);
            }
        } catch (RedisConnectionFailureException e) {
            handleConnectionFailure(e);
        } catch (Exception e) {
            log.warn("Redis error during CLEAR operation: {}", e.getMessage());
        }
    }

    /**
     * Get cache statistics
     * Returns empty stats if Redis is unavailable
     */
    public CacheStats getStats() {
        if (redisTemplate == null || !isAvailable()) {
            log.debug("Redis unavailable - returning empty stats");
            return new CacheStats(0, 0L, 0L, 0.0, connectionState.get().name());
        }

        try {
            Long hits = getStatValue(STATS_HIT_KEY);
            Long misses = getStatValue(STATS_MISS_KEY);
            long total = hits + misses;
            double hitRate = total > 0 ? (double) hits / total * 100 : 0;

            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            int size = keys != null ? keys.size() : 0;

            return new CacheStats(size, hits, misses, hitRate, connectionState.get().name());
        } catch (Exception e) {
            log.warn("Redis error during GET_STATS operation: {}", e.getMessage());
            return new CacheStats(0, 0L, 0L, 0.0, connectionState.get().name());
        }
    }

    /**
     * Reset statistics
     * Silently skips if Redis is unavailable
     */
    public void resetStats() {
        if (redisTemplate == null || !isAvailable()) {
            log.debug("Redis unavailable - Stats RESET skipped");
            return;
        }

        try {
            redisTemplate.delete(STATS_HIT_KEY);
            redisTemplate.delete(STATS_MISS_KEY);
            lastSuccessfulOperation = System.currentTimeMillis();
            log.info("Cache statistics reset");
        } catch (RedisConnectionFailureException e) {
            handleConnectionFailure(e);
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
        if (redisTemplate != null && isAvailable()) {
            try {
                redisTemplate.opsForValue().increment(STATS_HIT_KEY);
            } catch (Exception e) {
                log.debug("Redis error incrementing hit counter: {}", e.getMessage());
            }
        }
    }

    private void incrementMiss() {
        if (redisTemplate != null && isAvailable()) {
            try {
                redisTemplate.opsForValue().increment(STATS_MISS_KEY);
            } catch (Exception e) {
                log.debug("Redis error incrementing miss counter: {}", e.getMessage());
            }
        }
    }

    private Long getStatValue(String key) {
        if (redisTemplate == null || !isAvailable()) {
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
            double hitRate,
            String connectionState) {
    }
}
