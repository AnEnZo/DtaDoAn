package com.example.DtaAssigement.aop.exception;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Aspect for centralized exception handling
 * Logs all exceptions and tracks error metrics
 */
@Aspect
@Component
@Slf4j
public class ExceptionHandlingAspect {

    // Track error counts by method
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();

    // Track error counts by exception type
    private final Map<String, AtomicLong> errorTypesCounts = new ConcurrentHashMap<>();

    // Store recent errors for display (keep last 100)
    private final Map<String, ErrorRecord> recentErrors = new ConcurrentHashMap<>();
    private final AtomicLong errorIdGenerator = new AtomicLong(0);
    private static final int MAX_RECENT_ERRORS = 100;

    /**
     * Intercept all exceptions in service layer
     */
    @AfterThrowing(pointcut = "execution(* com.example.DtaAssigement.service..*.*(..))", throwing = "ex")
    public void handleServiceException(JoinPoint joinPoint, Exception ex) {
        handleException(joinPoint, ex, "SERVICE");
    }

    /**
     * Intercept all exceptions in controller layer
     */
    @AfterThrowing(pointcut = "execution(* com.example.DtaAssigement.controller..*.*(..))", throwing = "ex")
    public void handleControllerException(JoinPoint joinPoint, Exception ex) {
        handleException(joinPoint, ex, "CONTROLLER");
    }

    /**
     * Core exception handling logic
     */
    private void handleException(JoinPoint joinPoint, Exception ex, String layer) {
        String methodSignature = joinPoint.getSignature().toShortString();
        String exceptionType = ex.getClass().getSimpleName();
        String exceptionMessage = ex.getMessage();

        // Update error counts
        errorCounts.computeIfAbsent(methodSignature, k -> new AtomicLong(0))
                .incrementAndGet();
        errorTypesCounts.computeIfAbsent(exceptionType, k -> new AtomicLong(0))
                .incrementAndGet();

        // Store recent error
        long errorId = errorIdGenerator.incrementAndGet();
        ErrorRecord record = new ErrorRecord(
                errorId,
                methodSignature,
                layer,
                exceptionType,
                exceptionMessage,
                getStackTraceString(ex),
                getMethodArguments(joinPoint),
                LocalDateTime.now());

        // Keep only recent errors (simple FIFO)
        if (recentErrors.size() >= MAX_RECENT_ERRORS) {
            // Remove oldest (simple approach - not perfectly FIFO but good enough)
            String oldestKey = recentErrors.keySet().iterator().next();
            recentErrors.remove(oldestKey);
        }
        recentErrors.put(String.valueOf(errorId), record);

        // Log based on exception type
        if (isCriticalException(ex)) {
            log.error("[{}] CRITICAL ERROR in {}: {} - {}",
                    layer, methodSignature, exceptionType, exceptionMessage, ex);

            // In production, send alert here
            // alertService.sendCriticalAlert(methodSignature, ex);
        } else {
            log.warn("[{}] Exception in {}: {} - {}",
                    layer, methodSignature, exceptionType, exceptionMessage);
            log.debug("Exception details:", ex);
        }
    }

    /**
     * Determine if exception is critical
     */
    private boolean isCriticalException(Exception ex) {
        String exceptionType = ex.getClass().getSimpleName();

        // List of critical exception types
        return exceptionType.contains("Payment") ||
                exceptionType.contains("Security") ||
                exceptionType.contains("Authentication") ||
                exceptionType.contains("DataIntegrity") ||
                exceptionType.contains("NullPointer");
    }

    /**
     * Get stack trace as string (limited to 5 lines)
     */
    private String getStackTraceString(Exception ex) {
        StackTraceElement[] stackTrace = ex.getStackTrace();
        if (stackTrace.length == 0) {
            return "No stack trace available";
        }

        StringBuilder sb = new StringBuilder();
        int limit = Math.min(5, stackTrace.length);
        for (int i = 0; i < limit; i++) {
            sb.append(stackTrace[i].toString()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Get method arguments as string (for debugging)
     */
    private String getMethodArguments(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return "No arguments";
        }

        return Arrays.toString(args);
    }

    /**
     * Get error count for a specific method
     */
    public long getErrorCount(String methodSignature) {
        AtomicLong count = errorCounts.get(methodSignature);
        return count != null ? count.get() : 0;
    }

    /**
     * Get all error counts by method
     */
    public Map<String, Long> getAllErrorCounts() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        errorCounts.forEach((method, count) -> result.put(method, count.get()));
        return result;
    }

    /**
     * Get error counts by exception type
     */
    public Map<String, Long> getErrorCountsByType() {
        Map<String, Long> result = new ConcurrentHashMap<>();
        errorTypesCounts.forEach((type, count) -> result.put(type, count.get()));
        return result;
    }

    /**
     * Get recent errors
     */
    public Map<String, ErrorRecord> getRecentErrors() {
        return new ConcurrentHashMap<>(recentErrors);
    }

    /**
     * Reset all metrics (for testing)
     */
    public void resetMetrics() {
        errorCounts.clear();
        errorTypesCounts.clear();
        recentErrors.clear();
        log.info("Exception metrics reset");
    }

    /**
     * Record for storing error details
     */
    public record ErrorRecord(
            long errorId,
            String methodSignature,
            String layer,
            String exceptionType,
            String exceptionMessage,
            String stackTrace,
            String methodArguments,
            LocalDateTime timestamp) {
    }
}
