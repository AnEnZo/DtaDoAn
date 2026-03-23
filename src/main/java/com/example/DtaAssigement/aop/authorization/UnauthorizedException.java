package com.example.DtaAssigement.aop.authorization;

/**
 * Exception thrown when user doesn't have required role
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String requiredRole, String currentRoles) {
        super(String.format("Access denied. Required role: %s, Current roles: %s",
                requiredRole, currentRoles));
    }
}
