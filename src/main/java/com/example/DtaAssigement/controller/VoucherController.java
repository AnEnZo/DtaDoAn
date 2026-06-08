package com.example.DtaAssigement.controller;

import com.example.DtaAssigement.dto.VoucherListWithPointsDTO;
import com.example.DtaAssigement.entity.User;
import com.example.DtaAssigement.entity.Voucher;
import com.example.DtaAssigement.repository.UserRepository;
import com.example.DtaAssigement.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@Tag(name = "Voucher", description = "Voucher management endpoints")
public class VoucherController {

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @Operation(summary = "Get all vouchers", description = "Returns all vouchers, optionally with user reward points")
    public ResponseEntity<?> getAllVouchers(@RequestParam(required = false) String username) {
        List<Voucher> vouchers = voucherService.getAllVouchers();

        if (username != null && !username.isEmpty()) {
            User user = userRepository.findByUsername(username).orElse(null);
            int points = (user != null && user.getRewardPoints() != null) ? user.getRewardPoints() : 0;
            return ResponseEntity.ok(new VoucherListWithPointsDTO(vouchers, points));
        }

        return ResponseEntity.ok(vouchers);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    @Operation(summary = "Get voucher by ID")
    public ResponseEntity<Voucher> getVoucherById(@PathVariable Long id) {
        return voucherService.getVoucherById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Create voucher", description = "Create a voucher with imageUrl already provided (image must be uploaded to Cloudinary separately)")
    public ResponseEntity<Voucher> createVoucher(@RequestBody @Valid Voucher voucher) {
        return ResponseEntity.ok(voucherService.createVoucher(voucher));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Update voucher", description = "Update a voucher with imageUrl already provided (image must be uploaded to Cloudinary separately)")
    public ResponseEntity<Voucher> updateVoucher(@PathVariable Long id, @RequestBody Voucher voucher) {
        return ResponseEntity.ok(voucherService.updateVoucher(id, voucher));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Delete voucher")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long id) {
        voucherService.deleteVoucher(id);
        return ResponseEntity.noContent().build();
    }
}
