package com.example.DtaAssigement.config;

import com.example.DtaAssigement.aop.authorization.UnauthorizedException;
import com.example.DtaAssigement.aop.ratelimit.RateLimitException;
import com.example.DtaAssigement.aop.validation.ValidationException;
import com.example.DtaAssigement.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Centralized exception handling. Every handler returns a structured
 * {@link ErrorResponse} object (serialized by Jackson as application/json),
 * guaranteeing a consistent, always-parseable error contract for the frontend.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * When true (typically only the "dev" profile), 500 responses include the
     * exception class and stack trace. Defaults to false so production never
     * leaks internal details to the browser.
     */
    @Value("${app.expose-error-details:false}")
    private boolean exposeErrorDetails;

    private static final String DEFAULT_ERROR_MESSAGE = "Đã có lỗi xảy ra, vui lòng thử lại sau";

    /**
     * Build a standard error body. Never emits a literal "null" message.
     */
    private ErrorResponse build(HttpStatus status, String message, HttpServletRequest request) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message != null && !message.isBlank() ? message : DEFAULT_ERROR_MESSAGE)
                .path(request != null ? request.getRequestURI() : null)
                .build();
    }

    /**
     * Handler for validation errors (e.g., @Valid on @RequestBody)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> {
                    String fieldName = convertFieldName(error.getField());
                    String message = error.getDefaultMessage() != null ? error.getDefaultMessage() : "Giá trị không hợp lệ";
                    return fieldName + ": " + message;
                })
                .collect(Collectors.joining("; "));

        if (errorMessage.isEmpty()) {
            errorMessage = "Dữ liệu không hợp lệ";
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build(HttpStatus.BAD_REQUEST, errorMessage, request));
    }

    /**
     * Handler for type mismatch errors (e.g., invalid enum values)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Giá trị '%s' không hợp lệ cho trường '%s'",
                ex.getValue(), convertFieldName(ex.getName()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build(HttpStatus.BAD_REQUEST, message, request));
    }

    /**
     * Handler for malformed JSON or unreadable request body
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = "Dữ liệu gửi lên không hợp lệ";
        Throwable cause = ex.getCause();
        if (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null && causeMessage.contains("Cannot deserialize")) {
                message = "Định dạng dữ liệu không đúng";
            } else if (causeMessage != null && causeMessage.contains("com.fasterxml.jackson")) {
                message = "Dữ liệu JSON không hợp lệ";
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build(HttpStatus.BAD_REQUEST, message, request));
    }

    /**
     * Convert snake_case or camelCase field names to Vietnamese field names
     */
    private String convertFieldName(String fieldName) {
        return switch (fieldName) {
            case "displayName" -> "Tên hiển thị";
            case "phoneNumber" -> "Số điện thoại";
            case "email" -> "Email";
            case "username" -> "Tên đăng nhập";
            case "password" -> "Mật khẩu";
            case "rewardPoints" -> "Điểm thưởng";
            case "role", "roles" -> "Quyền";
            case "provider" -> "Nhà cung cấp";
            default -> fieldName;
        };
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String reason = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity.status(status).body(build(status, reason, request));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(HttpStatus.CONFLICT, ex.getMessage(), request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(build(HttpStatus.CONFLICT,
                        "Không thể xóa vì dữ liệu liên quan vẫn còn tồn tại.", request));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex, HttpServletRequest request) {
        String message = ex.getMessage() != null ? "Không tìm thấy: " + ex.getMessage() : "Không tìm thấy dữ liệu";
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build(HttpStatus.NOT_FOUND, message, request));
    }

    @ExceptionHandler(org.springframework.security.core.userdetails.UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(
            org.springframework.security.core.userdetails.UsernameNotFoundException ex, HttpServletRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Không tìm thấy người dùng";
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build(HttpStatus.NOT_FOUND, message, request));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .body(build(HttpStatus.NOT_ACCEPTABLE,
                        "Định dạng phản hồi không được hỗ trợ. Vui lòng yêu cầu với application/json.", request));
    }

    /**
     * Authorization failure (user lacks required role) -> 403, not 500.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build(HttpStatus.FORBIDDEN, ex.getMessage(), request));
    }

    /**
     * Rate limit exceeded -> 429 with a Retry-After header so clients can back off.
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitException ex, HttpServletRequest request) {
        long retryAfterSeconds = Math.max(0,
                ex.getRetryAfter().getEpochSecond() - Instant.now().getEpochSecond());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds))
                .body(build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request));
    }

    /**
     * Custom validation failure from the validation aspect -> 400.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleCustomValidation(ValidationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(build(HttpStatus.BAD_REQUEST, ex.getMessage(), request));
    }

    /**
     * Catch-all. The stack trace is logged server-side, never sent to the client
     * unless detailed error exposure is explicitly enabled.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}: {}",
                request != null ? request.getMethod() : "?",
                request != null ? request.getRequestURI() : "?",
                ex.getMessage(), ex);

        ErrorResponse body = build(HttpStatus.INTERNAL_SERVER_ERROR, DEFAULT_ERROR_MESSAGE, request);

        if (exposeErrorDetails) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            body.setMessage(ex.getMessage() != null ? ex.getMessage() : DEFAULT_ERROR_MESSAGE);
            body.setException(ex.getClass().getName());
            body.setMethod(request != null ? request.getMethod() : null);
            body.setStackTrace(sw.toString());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
