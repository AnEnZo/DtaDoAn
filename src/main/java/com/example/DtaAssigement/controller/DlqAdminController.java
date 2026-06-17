package com.example.DtaAssigement.controller;

import com.example.DtaAssigement.dto.queue.EmailQueueMessage;
import com.example.DtaAssigement.dto.queue.SmsQueueMessage;
import com.example.DtaAssigement.dto.queue.TrendingFoodUpdateMessage;
import com.example.DtaAssigement.entity.FailedMessage;
import com.example.DtaAssigement.repository.FailedMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sonus21.rqueue.core.RqueueMessageEnqueuer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dead Letter Queue Admin Controller
 * Manages failed messages in DLQ - view, retry, ignore
 */
@RestController
@RequestMapping("/api/admin/dlq")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "DLQ Admin", description = "Dead Letter Queue Administration APIs")
public class DlqAdminController {

    private final FailedMessageRepository failedMessageRepository;
    private final RqueueMessageEnqueuer rqueueMessageEnqueuer;
    private final ObjectMapper objectMapper;

    /**
     * Get all failed messages for a specific queue
     */
    @GetMapping("/{queueName}")
    @Operation(summary = "Get DLQ messages by queue name")
    public ResponseEntity<List<FailedMessage>> getDlqMessages(
            @PathVariable String queueName,
            @RequestParam(defaultValue = "PENDING") String status) {

        List<FailedMessage> messages = failedMessageRepository
                .findByQueueNameAndStatus(queueName, status);

        return ResponseEntity.ok(messages);
    }

    /**
     * Get all failed messages across all queues
     */
    @GetMapping("/all")
    @Operation(summary = "Get all DLQ messages")
    public ResponseEntity<List<FailedMessage>> getAllDlqMessages(
            @RequestParam(required = false) String status) {

        List<FailedMessage> messages = status != null
                ? failedMessageRepository.findByStatus(status)
                : failedMessageRepository.findAll();

        return ResponseEntity.ok(messages);
    }

    /**
     * Get DLQ statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get DLQ statistics")
    public ResponseEntity<Map<String, Object>> getDlqStats() {
        Map<String, Object> stats = new HashMap<>();

        // Count by queue
        stats.put("email-dlq", failedMessageRepository.countByQueueNameAndStatus("email-dlq", "PENDING"));
        stats.put("sms-dlq", failedMessageRepository.countByQueueNameAndStatus("sms-dlq", "PENDING"));
        stats.put("trending-food-dlq",
                failedMessageRepository.countByQueueNameAndStatus("trending-food-dlq", "PENDING"));

        // Total counts
        stats.put("total-pending", failedMessageRepository.countByStatus("PENDING"));
        stats.put("total-reprocessed", failedMessageRepository.countByStatus("REPROCESSED"));
        stats.put("total-ignored", failedMessageRepository.countByStatus("IGNORED"));

        return ResponseEntity.ok(stats);
    }

    /**
     * Retry a specific failed message
     */
    @PostMapping("/retry/{id}")
    @Operation(summary = "Retry a failed message")
    public ResponseEntity<?> retryMessage(@PathVariable Long id) {
        FailedMessage failedMessage = failedMessageRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Failed message: " + id));

        try {
            // Deserialize and re-enqueue based on message type
            Object message = deserializeMessage(failedMessage);
            String targetQueue = getTargetQueue(failedMessage.getQueueName());

            rqueueMessageEnqueuer.enqueue(targetQueue, message);

            // Update status
            failedMessage.setStatus("REPROCESSED");
            failedMessage.setReprocessedAt(LocalDateTime.now());
            failedMessageRepository.save(failedMessage);

            log.info("✅ Reprocessed message ID {} to queue {}", id, targetQueue);

            return ResponseEntity.ok(Map.of("message", "Message reprocessed successfully"));

        } catch (Exception e) {
            log.error("Failed to reprocess message ID {}", id, e);
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Không thể xử lý lại message");
        }
    }

    /**
     * Retry all pending messages for a queue
     */
    @PostMapping("/retry-all/{queueName}")
    @Operation(summary = "Retry all pending messages for a queue")
    public ResponseEntity<Map<String, Object>> retryAll(@PathVariable String queueName) {
        List<FailedMessage> messages = failedMessageRepository
                .findByQueueNameAndStatus(queueName, "PENDING");

        int success = 0;
        int failed = 0;

        for (FailedMessage msg : messages) {
            try {
                retryMessage(msg.getId());
                success++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to retry message ID {}", msg.getId(), e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", messages.size());
        result.put("success", success);
        result.put("failed", failed);

        return ResponseEntity.ok(result);
    }

    /**
     * Ignore (archive) a failed message
     */
    @PostMapping("/ignore/{id}")
    @Operation(summary = "Ignore a failed message")
    public ResponseEntity<String> ignoreMessage(@PathVariable Long id) {
        FailedMessage failedMessage = failedMessageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Failed message not found"));

        failedMessage.setStatus("IGNORED");
        failedMessageRepository.save(failedMessage);

        log.info("Ignored message ID {}", id);

        return ResponseEntity.ok("Message marked as ignored");
    }

    /**
     * Delete a failed message
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a failed message")
    public ResponseEntity<String> deleteMessage(@PathVariable Long id) {
        failedMessageRepository.deleteById(id);
        log.info("Deleted failed message ID {}", id);
        return ResponseEntity.ok("Message deleted");
    }

    // Helper methods

    private Object deserializeMessage(FailedMessage failedMessage) throws Exception {
        return switch (failedMessage.getMessageType()) {
            case "EMAIL" -> objectMapper.readValue(failedMessage.getMessageJson(), EmailQueueMessage.class);
            case "SMS" -> objectMapper.readValue(failedMessage.getMessageJson(), SmsQueueMessage.class);
            case "TRENDING_UPDATE" ->
                objectMapper.readValue(failedMessage.getMessageJson(), TrendingFoodUpdateMessage.class);
            default -> throw new IllegalArgumentException("Unknown message type: " + failedMessage.getMessageType());
        };
    }

    private String getTargetQueue(String dlqName) {
        // Remove -dlq suffix to get original queue name
        return dlqName.replace("-dlq", "");
    }
}
