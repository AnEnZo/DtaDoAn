package com.example.DtaAssigement.aop.exception;

import com.example.DtaAssigement.aop.exception.ExceptionHandlingAspect.ErrorRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for viewing exception/error metrics
 * Accessible to admins for monitoring system errors
 */
@RestController
@RequestMapping("/api/admin/errors")
@RequiredArgsConstructor
public class ErrorMetricsController {

    private final ExceptionHandlingAspect exceptionHandlingAspect;

    /**
     * Get all error counts by method
     */
    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> getAllErrorCounts() {
        return ResponseEntity.ok(exceptionHandlingAspect.getAllErrorCounts());
    }

    /**
     * Get error counts by exception type
     */
    @GetMapping("/counts/by-type")
    public ResponseEntity<Map<String, Long>> getErrorCountsByType() {
        return ResponseEntity.ok(exceptionHandlingAspect.getErrorCountsByType());
    }

    /**
     * Get top 10 methods with most errors
     */
    @GetMapping("/top-errors")
    public ResponseEntity<List<Map.Entry<String, Long>>> getTopErrors() {
        List<Map.Entry<String, Long>> topErrors = exceptionHandlingAspect.getAllErrorCounts()
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toList());

        return ResponseEntity.ok(topErrors);
    }

    /**
     * Get recent errors (last 100)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<ErrorRecord>> getRecentErrors() {
        List<ErrorRecord> errors = exceptionHandlingAspect.getRecentErrors()
                .values()
                .stream()
                .sorted(Comparator.comparing(ErrorRecord::timestamp).reversed())
                .collect(Collectors.toList());

        return ResponseEntity.ok(errors);
    }

    /**
     * Get specific error details by ID
     */
    @GetMapping("/{errorId}")
    public ResponseEntity<ErrorRecord> getErrorDetails(@PathVariable String errorId) {
        ErrorRecord error = exceptionHandlingAspect.getRecentErrors().get(errorId);
        if (error == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(error);
    }

    /**
     * Reset error metrics (for testing/debugging)
     */
    @PostMapping("/reset")
    public ResponseEntity<String> resetMetrics() {
        exceptionHandlingAspect.resetMetrics();
        return ResponseEntity.ok("Error metrics reset successfully");
    }
}
