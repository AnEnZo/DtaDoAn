package com.example.DtaAssigement.aop.caching;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing cache
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CustomCacheManager cacheManager;

    /**
     * Get cache statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<CustomCacheManager.CacheStats> getStats() {
        return ResponseEntity.ok(cacheManager.getStats());
    }

    /**
     * Clear entire cache
     */
    @PostMapping("/clear")
    public ResponseEntity<String> clearCache() {
        cacheManager.clear();
        return ResponseEntity.ok("Cache cleared successfully");
    }

    /**
     * Evict specific cache key
     */
    @DeleteMapping("/{key}")
    public ResponseEntity<String> evictKey(@PathVariable String key) {
        cacheManager.evict(key);
        return ResponseEntity.ok("Cache key evicted: " + key);
    }

    /**
     * Clean up expired entries
     */
    @PostMapping("/cleanup")
    public ResponseEntity<String> cleanupExpired() {
        cacheManager.cleanupExpired();
        return ResponseEntity.ok("Expired entries cleaned up");
    }

    /**
     * Reset statistics
     */
    @PostMapping("/stats/reset")
    public ResponseEntity<String> resetStats() {
        cacheManager.resetStats();
        return ResponseEntity.ok("Statistics reset successfully");
    }
}
