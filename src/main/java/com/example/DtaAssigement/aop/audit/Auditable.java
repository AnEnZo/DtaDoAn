package com.example.DtaAssigement.aop.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to mark methods that should be audited
 * 
 * Usage:
 * 
 * @Auditable(action = "CREATE_ORDER", entityType = "ORDER")
 *                   public Order createOrder(OrderDTO dto) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * The action being performed (e.g., CREATE, UPDATE, DELETE)
     */
    String action();

    /**
     * The type of entity being operated on (e.g., ORDER, INVOICE, USER)
     */
    String entityType();

    /**
     * Optional description of the auditable action
     */
    String description() default "";
}
