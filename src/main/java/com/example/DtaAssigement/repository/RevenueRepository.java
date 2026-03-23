package com.example.DtaAssigement.repository;

import com.example.DtaAssigement.entity.Revenue;
import com.example.DtaAssigement.ennum.PaymentMethod;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RevenueRepository extends JpaRepository<Revenue, Long> {

        // Find by Date and PaymentMethod for upsert logic in Service
        Optional<Revenue> findByDateAndPaymentMethod(LocalDate date, PaymentMethod paymentMethod);

        // Sum total revenue by date
        @Query("SELECT SUM(r.amount) FROM Revenue r WHERE r.date = :date")
        Double sumAmountByDate(@Param("date") LocalDate date);

        // Sum total revenue by month and year
        @Query("SELECT SUM(r.amount) FROM Revenue r WHERE MONTH(r.date) = :month AND YEAR(r.date) = :year")
        Double sumAmountByMonth(@Param("month") int month, @Param("year") int year);

        // Sum total revenue by payment method within a date range
        @Query("SELECT SUM(r.amount) FROM Revenue r WHERE r.paymentMethod = :method AND r.date BETWEEN :start AND :end")
        Double sumAmountByPaymentMethodBetween(@Param("method") PaymentMethod method, @Param("start") LocalDate start,
                        @Param("end") LocalDate end);

        // Group by payment method and sum amount within a date range
        // Returns List of Object[] { PaymentMethod, Double }
        @Query("SELECT r.paymentMethod, SUM(r.amount) FROM Revenue r WHERE r.date BETWEEN :start AND :end GROUP BY r.paymentMethod")
        List<Object[]> groupByPaymentMethodBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

        Optional<Revenue> findByDate(LocalDate date);

}
