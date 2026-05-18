package com.example.DtaAssigement.service;

import com.example.DtaAssigement.dto.InvoiceCalculationDTO;
import com.example.DtaAssigement.dto.CreateInvoiceResponse;
import com.example.DtaAssigement.ennum.PaymentMethod;
import com.example.DtaAssigement.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;

public interface InvoiceService {
        CreateInvoiceResponse createInvoice(Long orderId, String voucherCode, Long cashierId,
                        PaymentMethod paymentMethod,
                        String phoneNumber);

        Map<String, Object> createInvoiceWithCardPayment(Long orderId, String voucherCode, Long cashierId,
                        String cardType,
                        String phoneNumber);

        Map<String, Object> createInvoiceWithPayPal(Long orderId, String voucherCode, Long cashierId,
                        String phoneNumber);

        Map<String, Object> regeneratePayPalLink(Invoice invoice);

        Map<String, Object> processMomoWebhook(Map<String, String> ipnData);

        Map<String, Object> processPayPalWebhook(Map<String, Object> webhookData,
                        String transmissionId, String transmissionTime,
                        String certUrl, String authAlgo, String transmissionSig);

        boolean deleteInvoice(Long id);

        Optional<Invoice> getInvoiceById(Long id);

        Invoice findById(Long id);

        Page<Invoice> getAllInvoice(Pageable pageable);

        Page<Invoice> getInvoicesByDateRange(LocalDate start, LocalDate end, Pageable pageable);

        InvoiceCalculationDTO calculateInvoiceAmount(Long orderId, String voucherCode);

        void markInvoicePaid(Long invoiceId);

        Optional<Invoice> getInvoiceByOrderId(Long orderId);
}
