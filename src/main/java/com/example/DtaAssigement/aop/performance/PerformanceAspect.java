package com.example.DtaAssigement.aop.performance;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Aspect for performance monitoring
 * Measures execution time of all service and controller methods
 */
@Aspect
@Component
@Slf4j
public class PerformanceAspect {

    // Threshold for slow operations (in milliseconds)
    private static final long SLOW_THRESHOLD_MS = 1000;

    // Store metrics: method signature -> total time
    private final Map<String, AtomicLong> executionTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> executionCounts = new ConcurrentHashMap<>();

    /**
     * Monitor all service methods
     */
    @Around("execution(* com.example.DtaAssigement.service..*.*(..))")
    public Object monitorServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return measureExecutionTime(joinPoint, "SERVICE");
    }

    /**
     * Monitor all controller methods
     */
    @Around("execution(* com.example.DtaAssigement.controller..*.*(..))")
    public Object monitorControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return measureExecutionTime(joinPoint, "CONTROLLER");
    }

    /**
     * Core method to measure execution time
     */
    private Object measureExecutionTime(ProceedingJoinPoint joinPoint, String type) throws Throwable {
        String methodSignature = joinPoint.getSignature().toShortString();

        long startTime = System.currentTimeMillis();

        try {
            // Execute the actual method
            Object result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - startTime;

            // Update metrics
            updateMetrics(methodSignature, executionTime);

            // Log execution time
            if (executionTime > SLOW_THRESHOLD_MS) {
                log.warn("[{}] SLOW OPERATION: {} took {} ms",
                        type, methodSignature, executionTime);
            } else {
                log.debug("[{}] {} executed in {} ms",
                        type, methodSignature, executionTime);
            }

            return result;

        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[{}] {} failed after {} ms: {}",
                    type, methodSignature, executionTime, throwable.getMessage());
            throw throwable;
        }
    }

    /**
     * Update performance metrics
     */
    private void updateMetrics(String methodSignature, long executionTime) {
        executionTimes.computeIfAbsent(methodSignature, k -> new AtomicLong(0))
                .addAndGet(executionTime);
        executionCounts.computeIfAbsent(methodSignature, k -> new AtomicLong(0))
                .incrementAndGet();
    }

    /**
     * Get average execution time for a method
     */
    public long getAverageExecutionTime(String methodSignature) {
        AtomicLong totalTime = executionTimes.get(methodSignature);
        AtomicLong count = executionCounts.get(methodSignature);

        if (totalTime == null || count == null || count.get() == 0) {
            return 0;
        }

        return totalTime.get() / count.get();
    }

    /**
     * Get all metrics (for admin dashboard)
     */
    public Map<String, PerformanceMetrics> getAllMetrics() {
        Map<String, PerformanceMetrics> metrics = new ConcurrentHashMap<>();

        executionCounts.forEach((method, count) -> {
            long totalTime = executionTimes.getOrDefault(method, new AtomicLong(0)).get();
            long avgTime = count.get() > 0 ? totalTime / count.get() : 0;

            metrics.put(method, new PerformanceMetrics(
                    method,
                    count.get(),
                    totalTime,
                    avgTime));
        });

        return metrics;
    }

    /**
     * Reset all metrics (useful for testing)
     */
    public void resetMetrics() {
        executionTimes.clear();
        executionCounts.clear();
        log.info("Performance metrics reset");
    }

    /**
     * Simple DTO for performance metrics
     */
    public record PerformanceMetrics(
            String methodSignature,
            long executionCount,
            long totalExecutionTime,
            long averageExecutionTime) {
    }
}
