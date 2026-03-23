package com.example.DtaAssigement.controller;

import com.example.DtaAssigement.dto.VoucherListWithPointsDTO;
import com.example.DtaAssigement.entity.User;
import com.example.DtaAssigement.entity.Voucher;
import com.example.DtaAssigement.repository.UserRepository;
import com.example.DtaAssigement.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    public ResponseEntity<?> getAllVouchers(@RequestParam(required = false) String username) {
        List<Voucher> vouchers = voucherService.getAllVouchers();

        // If username is provided, return with user points
        if (username != null && !username.isEmpty()) {
            User user = userRepository.findByUsername(username).orElse(null);
            int points = (user != null && user.getRewardPoints() != null) ? user.getRewardPoints() : 0;
            return ResponseEntity.ok(new VoucherListWithPointsDTO(vouchers, points));
        }

        // Otherwise, return just vouchers (for admin/staff)
        return ResponseEntity.ok(vouchers);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    public ResponseEntity<Voucher> getVoucherById(@PathVariable Long id) {
        return voucherService.getVoucherById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Voucher> createVoucher(@RequestBody Voucher voucher) {
        return ResponseEntity.ok(voucherService.createVoucher(voucher));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Voucher> updateVoucher(@PathVariable Long id, @RequestBody Voucher voucher) {
        return ResponseEntity.ok(voucherService.updateVoucher(id, voucher));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.noContent().build();
    }
}
