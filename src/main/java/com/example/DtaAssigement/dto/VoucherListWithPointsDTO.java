package com.example.DtaAssigement.dto;

import com.example.DtaAssigement.entity.Voucher;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class VoucherListWithPointsDTO {
    private List<Voucher> vouchers;
    private int userPoints;
}
