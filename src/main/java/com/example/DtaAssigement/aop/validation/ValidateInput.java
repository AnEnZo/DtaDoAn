package com.example.DtaAssigement.aop.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to trigger input validation before method execution
 * Uses Bean Validation annotations on parameters
 *
 * Usage:
 * 
 * @ValidateInput
 *                public Order createOrder(@NotNull Long tableId) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidateInput {
}
