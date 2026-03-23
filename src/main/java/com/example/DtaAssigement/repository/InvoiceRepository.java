package com.example.DtaAssigement.repository;

import com.example.DtaAssigement.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Page<Invoice> findByPaymentTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Optional<Invoice> findByOrder_Id(Long orderId);

    Optional<Invoice> findByMomoOrderId(UUID momoOrderId);

    Optional<Invoice> findByPaypalOrderId(String paypalOrderId);

    @Query("select i.paymentMethod, count(i) from Invoice i where i.paymentTime between :start and :end group by i.paymentMethod")
    List<Object[]> countByPaymentMethodBetween(@Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
