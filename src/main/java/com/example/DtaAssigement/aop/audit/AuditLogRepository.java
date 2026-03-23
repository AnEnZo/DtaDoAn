package com.example.DtaAssigement.aop.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entity
 * Provides methods to query audit logs by various criteria
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find all audit logs for a specific user
     */
    @Query("SELECT a FROM AuditLog a WHERE a.username = :username ORDER BY a.timestamp DESC")
    List<AuditLog> findByUsernameOrderByTimestampDesc(@Param("username") String username);

    /**
     * Find audit logs by entity type
     */
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType ORDER BY a.timestamp DESC")
    List<AuditLog> findByEntityTypeOrderByTimestampDesc(@Param("entityType") String entityType);

    /**
     * Find audit logs for a specific entity
     */
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.timestamp DESC")
    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId);

    /**
     * Find audit logs within a time range
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :start AND :end ORDER BY a.timestamp DESC")
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Find recent audit logs for a user
     */
    @Query("SELECT a FROM AuditLog a WHERE a.username = :username AND a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentByUsername(@Param("username") String username, @Param("since") LocalDateTime since);

    /**
     * Find all actions of a specific type
     */
    @Query("SELECT a FROM AuditLog a WHERE a.action = :action ORDER BY a.timestamp DESC")
    List<AuditLog> findByActionOrderByTimestampDesc(@Param("action") String action);

    /**
     * Count audit logs by user
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.username = :username")
    long countByUsername(@Param("username") String username);

    /**
     * Count audit logs by action
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action = :action")
    long countByAction(@Param("action") String action);
}
