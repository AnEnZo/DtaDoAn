package com.example.DtaAssigement.controller;

import com.example.DtaAssigement.repository.TableRepository;
import com.example.DtaAssigement.repository.UserRepository;
import com.example.DtaAssigement.service.OrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@SecurityRequirement(name = "bearerAuth")
@AllArgsConstructor
public class DashboardController {

    private final OrderService orderService;
    private final TableRepository tableRepo;
    private final UserRepository userRepo;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public Map<String, Object> getSummary() {
        Map<String, Object> res = new HashMap<>();

        long ordersToday = orderService.countOrdersToday();
        long pendingOrders = orderService.countPendingOrders();

        long totalTables = tableRepo.countByDeletedFalse();
        long availableTables = tableRepo.countByAvailableTrueAndDeletedFalse();
        long tablesInUse = totalTables - availableTables;
        double utilizationPct = totalTables > 0 ? (tablesInUse * 100.0 / totalTables) : 0.0;

        YearMonth ym = YearMonth.now();
        LocalDateTime startMonth = ym.atDay(1).atStartOfDay();
        LocalDateTime endMonth = ym.atEndOfMonth().plusDays(1).atStartOfDay().minusNanos(1);
        long newCustomersThisMonth = userRepo.countByCreatedAtBetween(startMonth, endMonth);

        res.put("ordersToday", ordersToday);
        res.put("pendingOrders", pendingOrders);
        res.put("tablesInUse", tablesInUse);
        res.put("totalTables", totalTables);
        res.put("utilizationPct", utilizationPct);
        res.put("newCustomersThisMonth", newCustomersThisMonth);
        return res;
    }
}
