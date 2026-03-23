package com.example.DtaAssigement.repository;

import com.example.DtaAssigement.entity.FailedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Failed Message Repository
 * Repository for accessing failed messages in Dead Letter Queue
 */
@Repository
public interface FailedMessageRepository extends JpaRepository<FailedMessage, Long> {

    /**
     * Find failed messages by queue name and status
     */
    List<FailedMessage> findByQueueNameAndStatus(String queueName, String status);

    /**
     * Find failed messages by status
     */
    List<FailedMessage> findByStatus(String status);

    /**
     * Find failed messages by queue name
     */
    List<FailedMessage> findByQueueName(String queueName);

    /**
     * Count pending failed messages by queue
     */
    long countByQueueNameAndStatus(String queueName, String status);

    /**
     * Count failed messages by status (all queues)
     */
    long countByStatus(String status);
}
