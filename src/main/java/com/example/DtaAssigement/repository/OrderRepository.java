package com.example.DtaAssigement.repository;

import com.example.DtaAssigement.dto.ItemStatsDTO;
import com.example.DtaAssigement.entity.Order;
import com.example.DtaAssigement.ennum.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findById(@Param("id") Long id);

    // Unlocked read-only finder to avoid SELECT ... FOR UPDATE in read-only flows
    @Query("select o from Order o where o.id = :id")
    Optional<Order> readById(@Param("id") Long id);

    Optional<Order> findTopByTableIdOrderByOrderTimeDesc(Long tableId);

    Page<Order> findByOrderTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    long countByStatus(OrderStatus status);
    long countByOrderTimeBetween(LocalDateTime start, LocalDateTime end);

}
