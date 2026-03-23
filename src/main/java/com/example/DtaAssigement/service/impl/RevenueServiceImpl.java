package com.example.DtaAssigement.service.impl;

import com.example.DtaAssigement.dto.DailyRevenueDTO;
import com.example.DtaAssigement.dto.PaymentMethodCountDTO;
import com.example.DtaAssigement.dto.RevenueSummary;
import com.example.DtaAssigement.ennum.PaymentMethod;
import com.example.DtaAssigement.entity.Invoice;
import com.example.DtaAssigement.entity.Revenue;
import com.example.DtaAssigement.repository.InvoiceRepository;
import com.example.DtaAssigement.repository.RevenueRepository;
import com.example.DtaAssigement.service.RevenueService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class RevenueServiceImpl implements RevenueService {

    private final RevenueRepository revenueRepo;
    private final InvoiceRepository invoiceRepository;

    @Override
    public void recordRevenue(LocalDate date, PaymentMethod method, BigDecimal amount) {
        // Upsert Logic: Find existing record, if present update amount, else create new
        revenueRepo.findByDateAndPaymentMethod(date, method)
                .ifPresentOrElse(
                        existingRevenue -> {
                            existingRevenue.setAmount(existingRevenue.getAmount().add(amount));
                            revenueRepo.save(existingRevenue);
                        },
                        () -> {
                            Revenue newRevenue = Revenue.builder()
                                    .date(date)
                                    .paymentMethod(method)
                                    .amount(amount)
                                    .build();
                            revenueRepo.save(newRevenue);
                        });
    }

    @Override
    public double getRevenueByDate(LocalDate date) {
        Double total = revenueRepo.sumAmountByDate(date);
        return total != null ? total : 0.0;
    }

    @Override
    public double getRevenueByMonth(int month, int year) {
        Double total = revenueRepo.sumAmountByMonth(month, year);
        return total != null ? total : 0.0;
    }

    @Override
    public double getRevenueByPaymentMethod(PaymentMethod method, LocalDate start, LocalDate end) {
        Double total = revenueRepo.sumAmountByPaymentMethodBetween(method, start, end);
        return total != null ? total : 0.0;
    }

    @Override
    public List<RevenueSummary> getRevenueGroupedByMethod(LocalDate start, LocalDate end) {
        // Repository returns List<Object[]>: [PaymentMethod, Double]
        List<Object[]> results = revenueRepo.groupByPaymentMethodBetween(start, end);
        if (results == null)
            return Collections.emptyList();

        return results.stream()
                .map(arr -> new RevenueSummary(
                        (PaymentMethod) arr[0],
                        ((Number) arr[1]).doubleValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Invoice> getInvoicesByDate(LocalDate date) {
        return revenueRepo.findByDate(date)
                .map(Revenue::getInvoices)
                .orElseGet(Collections::emptyList);
    }

    @Override
    public List<DailyRevenueDTO> getDailyRevenueInMonth(int month, int year) {
        List<DailyRevenueDTO> result = new ArrayList<>();
        YearMonth yearMonth = YearMonth.of(year, month);
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = LocalDate.of(year, month, day);
            Double amount = revenueRepo.sumAmountByDate(date);
            result.add(new DailyRevenueDTO(date, amount != null ? amount : 0.0));
        }
        return result;
    }

    @Override
    public List<PaymentMethodCountDTO> getInvoiceCountsByPaymentMethod(LocalDate start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.plusDays(1).atStartOfDay().minusNanos(1);
        List<Object[]> rows = invoiceRepository.countByPaymentMethodBetween(startDt, endDt);
        List<PaymentMethodCountDTO> out = new ArrayList<>();
        for (Object[] r : rows) {
            PaymentMethod method = (PaymentMethod) r[0];
            long cnt = ((Number) r[1]).longValue();
            out.add(new PaymentMethodCountDTO(method, cnt));
        }
        return out;
    }
}
