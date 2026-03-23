package com.example.DtaAssigement.aop.authorization;

import com.example.DtaAssigement.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Aspect for role-based authorization
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthorizationAspect {

    private final JwtTokenUtil jwtTokenUtil;

    /**
     * Check authorization before method execution
     */
    @Before("@annotation(RequiresRole)")
    public void checkAuthorization(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresRole requiresRole = method.getAnnotation(RequiresRole.class);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User not authenticated");
        }
        
        String requiredRole = "ROLE_" + requiresRole.value();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        
        // Check if user has required role
        boolean hasRole = authorities.stream()
            .anyMatch(auth -> auth.getAuthority().equals(requiredRole));
        
        if (hasRole) {
            log.debug("Authorization passed: user has {}", requiredRole);
            return;
        }
        
        // If allowSelf is true, check if user is accessing their own data
        if (requiresRole.allowSelf()) {
            Long currentUserId = getCurrentUserId();
            Long targetUserId = extractUserIdFromArguments(joinPoint);
            
            if (currentUserId != null && currentUserId.equals(targetUserId)) {
                log.debug("Authorization passed: user accessing own data (ID: {})", currentUserId);
                return;
            }
        }
        
        // Authorization failed
        String currentRoles = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(", "));
        
        log.warn("Authorization FAILED: required={}, current={}", requiredRole, currentRoles);
        throw new UnauthorizedException(requiredRole, currentRoles);
    }

    /**
     * Get current user ID from JWT token
     */
    private Long getCurrentUserId() {
        try {
            String username = jwtTokenUtil.getCurrentUsername();
            // Assuming getUserIdByUsername method exists - adjust as needed
            // For now, return null if can't determine
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract user ID from method arguments
     * Assumes first Long parameter is user ID
     */
    private Long extractUserIdFromArguments(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }

        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }

        return null;
    }
}
