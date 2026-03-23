package com.example.DtaAssigement.dto;

import com.example.DtaAssigement.ennum.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMethodCountDTO {
    private PaymentMethod paymentMethod;
    private long count;
}
