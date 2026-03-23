package com.example.DtaAssigement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * Xem tất cả queues và số lượng jobs
     */
    @GetMapping("/status")
    @Operation(summary = "Xem trạng thái tất cả queues", description = "Trả về số lượng jobs đang pending trong từng queue")
    public ResponseEntity<Map<String, Object>> getQueueStatus() {
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
    }

    /**
     * Xóa TẤT CẢ jobs trong 1 queue cụ thể (pending + delayed + processing)
     */
    @DeleteMapping("/jobs/{queueName}")
    @Operation(summary = "Xóa tất cả jobs trong queue", description = "Xóa toàn bộ pending + delayed + processing jobs. Dùng để clear queue bị kẹt.")
    public ResponseEntity<Map<String, Object>> clearQueue(@PathVariable String queueName) {
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
    }

    /**
     * Xóa TẤT CẢ jobs trong TẤT CẢ queues được quản lý
     */
    @DeleteMapping("/jobs/all")
    @Operation(summary = "Xóa tất cả jobs trong tất cả queues", description = "⚠️ NGUY HIỂM: Xóa toàn bộ jobs pending/delayed/processing của tất cả queues.")
    public ResponseEntity<Map<String, Object>> clearAllQueues() {
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
    }
}
