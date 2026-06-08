package com.example.DtaAssigement.service.impl;


import com.example.DtaAssigement.entity.Voucher;
import com.example.DtaAssigement.repository.VoucherRepository;
import com.example.DtaAssigement.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VoucherServiceImpl implements VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    @Override
    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAll();
    }

    @Override
    public Optional<Voucher> getVoucherById(Long id) {
        return voucherRepository.findById(id);
    }

    @Override
    public Voucher createVoucher(Voucher voucher) {
        return voucherRepository.save(voucher);
    }

    @Override
    public Voucher updateVoucher(Long id, Voucher updatedVoucher) {
        return voucherRepository.findById(id)
                .map(voucher -> {
                    if (updatedVoucher.getCode() != null) {
                        voucher.setCode(updatedVoucher.getCode());
                    }
                    if (updatedVoucher.getType() != null) {
                        voucher.setType(updatedVoucher.getType());
                    }
                    if (updatedVoucher.getDiscountValue() != null) {
                        voucher.setDiscountValue(updatedVoucher.getDiscountValue());
                    }
                    if (updatedVoucher.getMinOrderAmount() != null) {
                        voucher.setMinOrderAmount(updatedVoucher.getMinOrderAmount());
                    }
                    if (updatedVoucher.getActive() != null) {
                        voucher.setActive(updatedVoucher.getActive());
                    }
                    if (updatedVoucher.getRequiredPoints() != null) {
                        voucher.setRequiredPoints(updatedVoucher.getRequiredPoints());
                    }
                    if (updatedVoucher.getImageUrl() != null) {
                        voucher.setImageUrl(updatedVoucher.getImageUrl());
                    }
                    return voucherRepository.save(voucher);
                })
                .orElseThrow(() -> new RuntimeException("Voucher not found"));
    }

    public void deleteVoucher(Long id) {
        voucherRepository.deleteById(id);
    }
}

