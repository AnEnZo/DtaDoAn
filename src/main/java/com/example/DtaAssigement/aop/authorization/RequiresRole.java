package com.example.DtaAssigement.aop.authorization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for role-based authorization
 * 
 * Usage:
 * @RequiresRole("ADMIN")
 * public void deleteUser(Long id) { ... }
 * 
 * @RequiresRole(value = "STAFF", allowSelf = true)
 *                     public void updateUser(Long userId, UserDTO dto) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {

    /**
     * Required role (e.g., "ADMIN", "STAFF", "USER")
     */
    String value();

    /**
     * Allow user to perform action on their own data
     * If true, will check if the first Long parameter matches current user ID
     */
    boolean allowSelf() default false;

    /**
     * Optional description
     */
    String description() default "";
}
