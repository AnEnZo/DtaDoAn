package com.example.DtaAssigement.aop.transaction;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect for logging database transactions
 * Monitors all @Transactional methods
 */
@Aspect
@Component
@Slf4j
public class TransactionLoggingAspect {

    private static final long SLOW_TRANSACTION_THRESHOLD_MS = 5000;

    /**
     * Log transaction lifecycle
     */
    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object logTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();

        log.debug("Transaction START: {}", methodName);
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            if (duration > SLOW_TRANSACTION_THRESHOLD_MS) {
                log.warn("Transaction COMMIT (SLOW): {} took {} ms", methodName, duration);
            } else {
                log.debug("Transaction COMMIT: {} took {} ms", methodName, duration);
            }

            return result;

        } catch (Throwable throwable) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Transaction ROLLBACK: {} after {} ms - {}",
                    methodName, duration, throwable.getMessage());
            throw throwable;
        }
    }
}
