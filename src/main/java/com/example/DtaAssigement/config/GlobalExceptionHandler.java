package com.example.DtaAssigement.config;

import com.example.DtaAssigement.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handler for validation errors (e.g., @Valid on @RequestBody)
     * Returns concise, user-friendly error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
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

        String response = String.format("""
        {
            "timestamp": "%s",
            "status": 400,
            "error": "Bad Request",
            "message": "%s",
            "path": "%s"
        }
        """,
                LocalDateTime.now(),
                errorMessage,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handler for type mismatch errors (e.g., invalid enum values)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<String> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Giá trị '%s' không hợp lệ cho trường '%s'",
                ex.getValue(), convertFieldName(ex.getName()));

        String response = String.format("""
        {
            "timestamp": "%s",
            "status": 400,
            "error": "Bad Request",
            "message": "%s",
            "path": "%s"
        }
        """,
                LocalDateTime.now(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handler for malformed JSON or unreadable request body
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
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

        String response = String.format("""
        {
            "timestamp": "%s",
            "status": 400,
            "error": "Bad Request",
            "message": "%s",
            "path": "%s"
        }
        """,
                LocalDateTime.now(),
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
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

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex, HttpServletRequest request) {
        String reason = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        int status = ex.getStatusCode().value();
        String errorName = "Error";
        try {
            HttpStatus httpStatus = HttpStatus.resolve(status);
            if (httpStatus != null) {
                errorName = httpStatus.getReasonPhrase();
            }
        } catch (Exception ignored) {}

        String response = String.format("""
        {
            "timestamp": "%s",
            "status": %d,
            "error": "%s",
            "message": "%s",
            "path": "%s"
        }
        """,
                LocalDateTime.now(),
                status,
                errorName,
                reason,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception ex, HttpServletRequest request) {
        // Lấy chi tiết stack trace
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();

        // Trả về chi tiết lỗi dạng JSON
        String response = String.format("""
        {
            "timestamp": "%s",
            "status": 500,
            "error": "Internal Server Error",
            "message": "%s",
            "path": "%s",
            "exception": "%s",
            "stackTrace": "%s"
        }
        """,
                LocalDateTime.now(),
                ex.getMessage(),
                request.getRequestURI(),
                ex.getClass().getName(),
                stackTrace
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }


    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<String> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        return new ResponseEntity<>(
                "Định dạng phản hồi không được hỗ trợ. Vui lòng yêu cầu với application/json.",
                new HttpHeaders(),
                HttpStatus.NOT_ACCEPTABLE
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        String response = String.format("""
        {
            "timestamp": "%s",
            "status": 409,
            "error": "Conflict",
            "message": "%s",
            "path": "%s"
        }
        """,
                LocalDateTime.now(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        String response = String.format("""
        {
            "timestamp": "%s",
            "status": 409,
            "error": "Conflict",
            "message": "Không thể xóa đơn hàng vì dữ liệu liên quan (hóa đơn) vẫn còn tồn tại.",
            "path": "%s"
        }
        """,
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("Không tìm thấy: " + ex.getMessage());
    }

}
