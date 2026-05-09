package com.example.DtaAssigement.controller;

import com.example.DtaAssigement.config.RedisConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Admin Controller để xem và xóa jobs đang tồn đọng trong RQueue (Redis).
 * Hỗ trợ cả pending queue và delayed queue.
 */
@RestController
@RequestMapping("/api/admin/rqueue")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
@Tag(name = "RQueue Admin", description = "Xem và xóa jobs trong RQueue (Redis)")
public class RQueueAdminController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConfig redisConfig;

    // Các queue names cần quản lý
    private static final List<String> MANAGED_QUEUES = List.of(
            "food-trend-batch-queue",
            "trending-food-queue",
            "email-queue",
            "sms-queue");

    // RQueue prefix patterns
    private static final String RQUEUE_PREFIX = "rqueue-";
    private static final String RQUEUE_DELAYED_PREFIX = "rqueue-delayed:";
    private static final String RQUEUE_PROCESSING_PREFIX = "rqueue-processing:";

    /**
     * Kiểm tra Redis có khả dụng không
     */
    private boolean isRedisAvailable() {
        return redisTemplate != null && redisConfig.isRedisConnected();
    }

    /**
     * Xem tất cả queues và số lượng jobs
     */
    @GetMapping("/status")
    @Operation(summary = "Xem trạng thái tất cả queues", description = "Trả về số lượng jobs đang pending trong từng queue")
    public ResponseEntity<Map<String, Object>> getQueueStatus() {
        if (!isRedisAvailable()) {
            log.warn("⚠️ Redis unavailable - cannot get queue status");
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Redis is not available",
                    "message", "Unable to connect to Redis server. Please check the connection and try again.",
                    "redisConnected", false
            ));
        }

        try {
            Map<String, Object> result = new LinkedHashMap<>();

            for (String queue : MANAGED_QUEUES) {
                Map<String, Object> queueInfo = new LinkedHashMap<>();

                // Pending queue (list key)
                String pendingKey = RQUEUE_PREFIX + queue;
                Long pendingSize = redisTemplate.opsForList().size(pendingKey);
                queueInfo.put("pending", pendingSize != null ? pendingSize : 0);

                // Delayed queue (zset key)
                String delayedKey = RQUEUE_DELAYED_PREFIX + queue;
                Long delayedSize = redisTemplate.opsForZSet().size(delayedKey);
                queueInfo.put("delayed", delayedSize != null ? delayedSize : 0);

                // Processing queue (zset key)
                String processingKey = RQUEUE_PROCESSING_PREFIX + queue;
                Long processingSize = redisTemplate.opsForZSet().size(processingKey);
                queueInfo.put("processing", processingSize != null ? processingSize : 0);

                result.put(queue, queueInfo);
            }

            log.info("📊 RQueue status requested");
            return ResponseEntity.ok(result);
        } catch (RedisConnectionFailureException e) {
            log.error("❌ Redis connection failed during getQueueStatus: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Redis connection failed",
                    "message", e.getMessage(),
                    "redisConnected", false
            ));
        }
    }

    /**
     * Xem chi tiết jobs trong 1 queue cụ thể (tối đa 50 items đầu)
     */
    @GetMapping("/jobs/{queueName}")
    @Operation(summary = "Xem jobs trong queue", description = "Trả về danh sách raw job messages trong pending queue (tối đa 50)")
    public ResponseEntity<Map<String, Object>> getQueueJobs(
            @PathVariable String queueName,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {

        if (!isRedisAvailable()) {
            log.warn("⚠️ Redis unavailable - cannot get queue jobs for: {}", queueName);
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Redis is not available",
                    "message", "Unable to connect to Redis server. Please check the connection and try again.",
                    "redisConnected", false
            ));
        }

        try {
            String pendingKey = RQUEUE_PREFIX + queueName;
            Long size = redisTemplate.opsForList().size(pendingKey);
            List<Object> jobs = redisTemplate.opsForList().range(pendingKey, offset, offset + limit - 1);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("queue", queueName);
            result.put("total_pending", size != null ? size : 0);
            result.put("offset", offset);
            result.put("returned", jobs != null ? jobs.size() : 0);
            result.put("jobs", jobs != null ? jobs : List.of());

            return ResponseEntity.ok(result);
        } catch (RedisConnectionFailureException e) {
            log.error("❌ Redis connection failed during getQueueJobs: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Redis connection failed",
                    "message", e.getMessage(),
                    "redisConnected", false
            ));
        }
    }

    /**
     * Xóa TẤT CẢ jobs trong 1 queue cụ thể (pending + delayed + processing)
     */
    @DeleteMapping("/jobs/{queueName}")
    @Operation(summary = "Xóa tất cả jobs trong queue", description = "Xóa toàn bộ pending + delayed + processing jobs. Dùng để clear queue bị kẹt.")
    public ResponseEntity<Map<String, Object>> clearQueue(@PathVariable String queueName) {
        if (!isRedisAvailable()) {
            log.warn("⚠️ Redis unavailable - cannot clear queue: {}", queueName);
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Redis is not available",
                    "message", "Unable to connect to Redis server. Please check the connection and try again.",
                    "redisConnected", false
            ));
        }

        try {
            Map<String, Object> result = new LinkedHashMap<>();

            String pendingKey = RQUEUE_PREFIX + queueName;
            String delayedKey = RQUEUE_DELAYED_PREFIX + queueName;
            String processingKey = RQUEUE_PROCESSING_PREFIX + queueName;

            Long pendingSize = redisTemplate.opsForList().size(pendingKey);
            Long delayedSize = redisTemplate.opsForZSet().size(delayedKey);
            Long processingSize = redisTemplate.opsForZSet().size(processingKey);

            redisTemplate.delete(pendingKey);
            redisTemplate.delete(delayedKey);
            redisTemplate.delete(processingKey);

            result.put("queue", queueName);
            result.put("deleted_pending", pendingSize != null ? pendingSize : 0);
            result.put("deleted_delayed", delayedSize != null ? delayedSize : 0);
            result.put("deleted_processing", processingSize != null ? processingSize : 0);
            result.put("status", "cleared");

            log.warn("🗑️ Cleared queue '{}': pending={}, delayed={}, processing={}",
                    queueName, pendingSize, delayedSize, processingSize);

            return ResponseEntity.ok(result);
        } catch (RedisConnectionFailureException e) {
            log.error("❌ Redis connection failed during clearQueue: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Redis connection failed",
                    "message", e.getMessage(),
                    "redisConnected", false
            ));
        }
    }

    /**
     * Xóa TẤT CẢ jobs trong TẤT CẢ queues được quản lý
     */
    @DeleteMapping("/jobs/all")
    @Operation(summary = "Xóa tất cả jobs trong tất cả queues", description = "⚠️ NGUY HIỂM: Xóa toàn bộ jobs pending/delayed/processing của tất cả queues.")
    public ResponseEntity<Map<String, Object>> clearAllQueues() {
        if (!isRedisAvailable()) {
            log.warn("⚠️ Redis unavailable - cannot clear all queues");
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Redis is not available",
                    "message", "Unable to connect to Redis server. Please check the connection and try again.",
                    "redisConnected", false
            ));
        }

        try {
            Map<String, Object> summary = new LinkedHashMap<>();
            int totalDeleted = 0;

            for (String queue : MANAGED_QUEUES) {
                String pendingKey = RQUEUE_PREFIX + queue;
                String delayedKey = RQUEUE_DELAYED_PREFIX + queue;
                String processingKey = RQUEUE_PROCESSING_PREFIX + queue;

                Long p = redisTemplate.opsForList().size(pendingKey);
                Long d = redisTemplate.opsForZSet().size(delayedKey);
                Long r = redisTemplate.opsForZSet().size(processingKey);

                redisTemplate.delete(pendingKey);
                redisTemplate.delete(delayedKey);
                redisTemplate.delete(processingKey);

                long count = (p != null ? p : 0) + (d != null ? d : 0) + (r != null ? r : 0);
                summary.put(queue, count + " jobs deleted");
                totalDeleted += (int) count;
            }

            log.warn("🗑️ Cleared ALL managed queues. Total jobs deleted: {}", totalDeleted);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "all_cleared");
            result.put("total_deleted", totalDeleted);
            result.put("queues", summary);

            return ResponseEntity.ok(result);
        } catch (RedisConnectionFailureException e) {
            log.error("❌ Redis connection failed during clearAllQueues: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of(
                    "error", "Redis connection failed",
                    "message", e.getMessage(),
                    "redisConnected", false
            ));
        }
    }

    /**
     * Kiểm tra trạng thái kết nối Redis
     */
    @GetMapping("/health")
    @Operation(summary = "Kiểm tra trạng thái Redis", description = "Trả về trạng thái kết nối Redis hiện tại")
    public ResponseEntity<Map<String, Object>> getRedisHealth() {
        boolean connected = redisConfig.isRedisConnected();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("redisConnected", connected);
        result.put("status", connected ? "healthy" : "unhealthy");

        if (!connected) {
            result.put("message", "Redis is not connected. Connection will be retried automatically every 30 seconds.");
            result.put("retryInfo", Map.of(
                    "autoRetry", true,
                    "retryIntervalSeconds", 30
            ));
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Thử kết nối lại Redis ngay lập tức
     */
    @PostMapping("/reconnect")
    @Operation(summary = "Thử kết nối lại Redis", description = "Thủ công kích hoạt retry kết nối Redis")
    public ResponseEntity<Map<String, Object>> reconnectRedis() {
        log.info("🔄 Manual Redis reconnection requested");
        redisConfig.refreshConnection();

        boolean connected = redisConfig.isRedisConnected();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("redisConnected", connected);
        result.put("status", connected ? "reconnected" : "still_disconnected");

        if (connected) {
            result.put("message", "Redis connection established successfully!");
        } else {
            result.put("message", "Failed to reconnect. Will continue retrying automatically every 30 seconds.");
        }

        return ResponseEntity.ok(result);
    }
}
