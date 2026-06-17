package com.example.DtaAssigement.controller;

import com.example.DtaAssigement.entity.UserVoucher;
import com.example.DtaAssigement.invoidGenerateWordExcelQrCode.QRCodeGenerator;
import com.example.DtaAssigement.service.UserVoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-vouchers")
@Slf4j
public class UserVoucherController {

    private final UserVoucherService userVoucherService;

    @GetMapping(value = "/{userVoucherId}/qrcode")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    public ResponseEntity<?> getVoucherQRCode(@PathVariable Long userVoucherId) {
        // Lấy UserVoucher từ DB
        UserVoucher userVoucher = userVoucherService.getById(userVoucherId);

        if (userVoucher == null) {
            throw new NoSuchElementException("voucher: " + userVoucherId);
        }

        try {
            // Sinh QR code từ mã code của UserVoucher
            BufferedImage qrImage = QRCodeGenerator.generateQRCodeImage(userVoucher.getCode());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imageBytes);
        } catch (Exception e) {
            log.error("Error generating voucher QR code for {}", userVoucherId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi tạo mã QR voucher");
        }
    }


    @PostMapping("/exchange")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    public ResponseEntity<UserVoucher> exchangeVoucher(
            @RequestParam String username,
            @RequestParam String voucherCode) {
        UserVoucher userVoucher = userVoucherService.exchangePointsForVoucher(username, voucherCode);
        return ResponseEntity.ok(userVoucher);
    }

    @GetMapping("/{username}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
    public ResponseEntity<List<UserVoucher>> getVouchersForUser(@PathVariable String username) {
        List<UserVoucher> vouchers = userVoucherService.getVouchersForUser(username);
        return ResponseEntity.ok(vouchers);
    }

}
