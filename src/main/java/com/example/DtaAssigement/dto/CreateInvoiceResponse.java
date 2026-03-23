package com.example.DtaAssigement.dto;

import com.example.DtaAssigement.entity.Invoice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInvoiceResponse {
    private Invoice invoice;
    private String payUrl;
    private String deeplink;
    private String deeplinkQr;
}
