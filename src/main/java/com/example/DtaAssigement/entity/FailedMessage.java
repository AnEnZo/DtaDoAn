package com.example.DtaAssigement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Failed Message Entity
 * Stores messages that failed processing and moved to Dead Letter Queue
 */
@Entity
@Table(name = "failed_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String queueName;

    @Column(nullable = false)
    private String messageType; // EMAIL, SMS, TRENDING_UPDATE

    @Column(columnDefinition = "TEXT", nullable = false)
    private String messageJson; // Serialized message

    @Column(columnDefinition = "TEXT")
    private String errorMessage; // Last error that caused failure

    @Column(nullable = false)
    private LocalDateTime failedAt;

    @Column(nullable = false)
    private Integer retryCount; // Number of retries before DLQ

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, REPROCESSED, IGNORED

    private LocalDateTime reprocessedAt;

    @PrePersist
    protected void onCreate() {
        if (failedAt == null) {
            failedAt = LocalDateTime.now();
        }
    }
}
