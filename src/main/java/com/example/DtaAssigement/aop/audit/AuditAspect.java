package com.example.DtaAssigement.aop.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspect for audit logging
 * Intercepts methods annotated with @Auditable and logs them
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    /**
     * Log successful method execution
     */
    @AfterReturning(pointcut = "@annotation(Auditable)", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        try {
            Auditable auditable = getAuditableAnnotation(joinPoint);
            if (auditable == null) {
                return;
            }

            String action = auditable.action();
            String entityType = auditable.entityType();
            Long entityId = extractEntityId(result);
            String details = buildDetails(joinPoint, result, auditable.description());

            auditService.log(action, entityType, entityId, details);

            log.info("Audit: {} - {} (ID: {})", action, entityType, entityId);

        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }

    /**
     * Log failed method execution
     */
    @AfterThrowing(pointcut = "@annotation(Auditable)", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Exception ex) {
        try {
            Auditable auditable = getAuditableAnnotation(joinPoint);
            if (auditable == null) {
                return;
            }

            String action = auditable.action() + "_FAILED";
            String entityType = auditable.entityType();
            String details = "Failed: " + ex.getMessage();

            auditService.log(action, entityType, null, details);

            log.warn("Audit: {} failed - {}", auditable.action(), ex.getMessage());

        } catch (Exception e) {
            log.error("Failed to create audit log for exception", e);
        }
    }

    /**
     * Get Auditable annotation from method
     */
    private Auditable getAuditableAnnotation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getAnnotation(Auditable.class);
    }

    /**
     * Extract entity ID from result
     * Assumes entity has getId() method
     */
    private Long extractEntityId(Object result) {
        if (result == null) {
            return null;
        }

        try {
            Method getIdMethod = result.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(result);

            if (id instanceof Long) {
                return (Long) id;
            } else if (id instanceof Integer) {
                return ((Integer) id).longValue();
            }
        } catch (Exception e) {
            // Entity doesn't have getId() method or failed to extract
            log.debug("Could not extract entity ID from result", e);
        }

        return null;
    }

    /**
     * Build details string from method arguments and result
     */
    private String buildDetails(JoinPoint joinPoint, Object result, String description) {
        StringBuilder details = new StringBuilder();

        if (description != null && !description.isEmpty()) {
            details.append(description);
        }

        // Add method name
        details.append(" [Method: ").append(joinPoint.getSignature().getName()).append("]");

        // Add argument info (excluding sensitive data)
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            details.append(" [Args count: ").append(args.length).append("]");
        }

        return details.toString();
    }
}
