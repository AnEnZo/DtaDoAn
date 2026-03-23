package com.example.DtaAssigement.aop.performance;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for viewing performance metrics
 * Accessible to admins for monitoring system performance
 */
@RestController
@RequestMapping("/api/admin/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceAspect performanceAspect;

    /**
     * Get all performance metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, PerformanceAspect.PerformanceMetrics>> getAllMetrics() {
        return ResponseEntity.ok(performanceAspect.getAllMetrics());
    }

    /**
     * Get top 10 slowest methods by average execution time
     */
    @GetMapping("/slowest")
    public ResponseEntity<List<PerformanceAspect.PerformanceMetrics>> getSlowestMethods() {
        List<PerformanceAspect.PerformanceMetrics> slowest = performanceAspect.getAllMetrics()
                .values()
                .stream()
                .sorted(Comparator.comparingLong(PerformanceAspect.PerformanceMetrics::averageExecutionTime).reversed())
                .limit(10)
                .collect(Collectors.toList());

        return ResponseEntity.ok(slowest);
    }

    /**
     * Get top 10 most frequently called methods
     */
    @GetMapping("/most-called")
    public ResponseEntity<List<PerformanceAspect.PerformanceMetrics>> getMostCalledMethods() {
        List<PerformanceAspect.PerformanceMetrics> mostCalled = performanceAspect.getAllMetrics()
                .values()
                .stream()
                .sorted(Comparator.comparingLong(PerformanceAspect.PerformanceMetrics::executionCount).reversed())
                .limit(10)
                .collect(Collectors.toList());

        return ResponseEntity.ok(mostCalled);
    }

    /**
     * Reset all metrics (for testing/debugging)
     */
    @PostMapping("/metrics/reset")
    public ResponseEntity<String> resetMetrics() {
        performanceAspect.resetMetrics();
        return ResponseEntity.ok("Metrics reset successfully");
    }
}
