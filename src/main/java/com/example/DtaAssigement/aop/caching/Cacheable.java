package com.example.DtaAssigement.aop.caching;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for caching method results
 * 
 * Usage:
 * 
 * @Cacheable(key = "allMenuItems", ttl = 600)
 *                public List<MenuItem> getAllMenuItems() { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cacheable {

    /**
     * Cache key name
     * If not specified, will use method name + arguments
     */
    String key() default "";

    /**
     * Time to live in seconds
     * Default: 300 seconds (5 minutes)
     */
    int ttl() default 300;

    /**
     * Optional description
     */
    String description() default "";
}
