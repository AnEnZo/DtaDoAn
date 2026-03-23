package com.example.DtaAssigement.aop.caching;

import com.example.DtaAssigement.aop.caching.CustomCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Aspect for caching method results
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class CachingAspect {

    private final CustomCacheManager cacheManager;

    /**
     * Intercept methods annotated with @Cacheable
     */
    @Around("@annotation(Cacheable)")
    public Object cacheMethodResult(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Cacheable cacheable = method.getAnnotation(Cacheable.class);

        // Generate cache key
        String cacheKey = generateCacheKey(cacheable, joinPoint);

        // Try to get from cache
        Object cachedResult = cacheManager.get(cacheKey);
        if (cachedResult != null) {
            log.debug("Returning cached result for: {}", cacheKey);
            return cachedResult;
        }

        // Execute method if not in cache
        Object result = joinPoint.proceed();

        // Store in cache
        if (result != null) {
            cacheManager.put(cacheKey, result, cacheable.ttl());
            log.debug("Cached result for: {} (TTL: {}s)", cacheKey, cacheable.ttl());
        }

        return result;
    }

    /**
     * Intercept methods annotated with @CacheEvict
     */
    @After("@annotation(CacheEvict)")
    public void evictCache(org.aspectj.lang.JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);

        // Evict specified cache keys
        String[] keys = cacheEvict.keys();
        cacheManager.evictAll(keys);

        log.debug("Evicted cache keys: {}", Arrays.toString(keys));
    }

    /**
     * Generate cache key from annotation and method call
     */
    private String generateCacheKey(Cacheable cacheable, ProceedingJoinPoint joinPoint) {
        String baseKey = cacheable.key();

        // If no key specified, use method name
        if (baseKey == null || baseKey.isEmpty()) {
            baseKey = joinPoint.getSignature().getName();
        }

        // Append arguments to make key unique per argument combination
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            StringBuilder keyBuilder = new StringBuilder(baseKey);
            for (Object arg : args) {
                keyBuilder.append("_").append(arg != null ? arg.toString() : "null");
            }
            return keyBuilder.toString();
        }

        return baseKey;
    }
}
