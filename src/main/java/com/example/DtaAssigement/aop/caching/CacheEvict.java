package com.example.DtaAssigement.aop.caching;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to evict/clear cache entries
 * Use on methods that modify data to invalidate cached results
 * 
 * Usage:
 * 
 * @CacheEvict(keys = {"allMenuItems", "menuByCategory"})
 *                  public MenuItem createMenuItem(MenuItemDTO dto) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheEvict {

    /**
     * Cache keys to evict
     * Can specify multiple keys to clear related caches
     */
    String[] keys();
}
