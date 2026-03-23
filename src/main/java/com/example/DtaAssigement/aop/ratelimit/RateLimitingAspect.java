package com.example.DtaAssigement.aop.ratelimit;

import com.example.DtaAssigement.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Aspect for rate limiting
 * Uses sliding window algorithm with in-memory storage
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitingAspect {

    private final JwtTokenUtil jwtTokenUtil;

    // Map: key -> RateLimitBucket
    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Check rate limit before method execution
     */
    @Before("@annotation(RateLimit)")
    public void checkRateLimit(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        String key = generateKey(rateLimit, joinPoint);

        RateLimitBucket bucket = buckets.computeIfAbsent(key,
                k -> new RateLimitBucket(rateLimit.maxRequests(), rateLimit.windowSeconds()));

        if (!bucket.tryConsume()) {
            Instant retryAfter = bucket.getWindowStart().plusSeconds(bucket.getWindowSeconds());
            log.warn("Rate limit exceeded for key: {}", key);
            throw new RateLimitException(rateLimit.maxRequests(), rateLimit.windowSeconds(), retryAfter);
        }

        log.debug("Rate limit check passed for key: {} ({}/{})",
                key, bucket.getCurrentCount(), bucket.getMaxRequests());
    }

    /**
     * Generate rate limit key
     */
    private String generateKey(RateLimit rateLimit, JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();

        if (rateLimit.perUser()) {
            String username = jwtTokenUtil.getCurrentUsername();
            return String.format("%s:%s", methodName, username);
        }

        return methodName;
    }

    /**
     * Rate limit bucket using sliding window
     */
    private static class RateLimitBucket {
        private final int maxRequests;
        private final int windowSeconds;
        private Instant windowStart;
        private final AtomicInteger currentCount;

        public RateLimitBucket(int maxRequests, int windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowSeconds = windowSeconds;
            this.windowStart = Instant.now();
            this.currentCount = new AtomicInteger(0);
        }

        public synchronized boolean tryConsume() {
            Instant now = Instant.now();

            // Reset window if expired
            if (now.isAfter(windowStart.plusSeconds(windowSeconds))) {
                windowStart = now;
                currentCount.set(0);
            }

            // Check if can consume
            if (currentCount.get() < maxRequests) {
                currentCount.incrementAndGet();
                return true;
            }

            return false;
        }

        public int getMaxRequests() {
            return maxRequests;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public Instant getWindowStart() {
            return windowStart;
        }

        public int getCurrentCount() {
            return currentCount.get();
        }
    }
}
