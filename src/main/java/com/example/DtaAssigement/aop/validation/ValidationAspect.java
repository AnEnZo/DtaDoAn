package com.example.DtaAssigement.aop.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aspect for input validation using Bean Validation
 */
@Aspect
@Component
@Slf4j
public class ValidationAspect {

    private final Validator validator;
    private final ExecutableValidator executableValidator;

    public ValidationAspect() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
        this.executableValidator = validator.forExecutables();
    }

    /**
     * Validate method parameters before execution
     */
    @Before("@annotation(ValidateInput)")
    public void validateParameters(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] arguments = joinPoint.getArgs();
        Object target = joinPoint.getTarget();

        // Validate method parameters
        Set<ConstraintViolation<Object>> violations = executableValidator.validateParameters(target, method, arguments);

        if (!violations.isEmpty()) {
            String errors = violations.stream()
                    .map(v -> String.format("%s: %s",
                            v.getPropertyPath(), v.getMessage()))
                    .collect(Collectors.joining(", "));

            log.warn("Validation failed for {}: {}", method.getName(), errors);
            throw new ValidationException("Validation failed: " + errors);
        }

        log.debug("Validation passed for {}", method.getName());
    }
}
