package com.example.DtaAssigement.aop.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for rate limiting method calls
 * 
 * Usage:
 * 
 * @RateLimit(maxRequests = 10, windowSeconds = 3600)
 *                        public void createOrder() { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Maximum number of requests allowed
     */
    int maxRequests();

    /**
     * Time window in seconds
     */
    int windowSeconds();

    /**
     * Apply rate limit per user (default: global)
     */
    boolean perUser() default false;
}
