package com.example.DtaAssigement.service.impl;

import com.example.DtaAssigement.aop.audit.Auditable;

import com.example.DtaAssigement.dto.InvoiceCalculationDTO;
import com.example.DtaAssigement.dto.CreateInvoiceResponse;
import com.example.DtaAssigement.ennum.PaymentMethod;
import com.example.DtaAssigement.entity.*;
import com.example.DtaAssigement.ennum.OrderStatus;
import com.example.DtaAssigement.ennum.InvoiceStatus;
import com.example.DtaAssigement.repository.*;
import com.example.DtaAssigement.service.InvoiceService;
import com.example.DtaAssigement.service.RevenueService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;

@Service
@Transactional
@AllArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final InvoiceRepository invoiceRepo;
    private final UserRepository userRepo;
    private final VoucherRepository voucherRepo;
    private final UserVoucherRepository userVoucherRepo;
    private final RevenueService revenueService;
    private final MomoClient momoClient;
    private final PayPalClient payPalClient;

    @Override
    @Auditable(action = "CREATE_INVOICE", entityType = "INVOICE", description = "Create invoice for order")
    public CreateInvoiceResponse createInvoice(Long orderId, String voucherCode, Long cashierId,
            PaymentMethod paymentMethod, String phoneNumber) {
        // Lấy đơn hàng và kiểm tra trạng thái
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        if (order.getStatus() != OrderStatus.SERVED) {
            throw new IllegalStateException("Only served orders can be paid");
        }
        // Nếu là chuyển khoản: kiểm tra đã có hóa đơn PENDING chưa, nếu có thì tái tạo
        // link MoMo trên hóa đơn cũ
        if (paymentMethod == PaymentMethod.TRANSFER) {
            Optional<Invoice> existingOpt = invoiceRepo.findByOrder_Id(orderId);
            if (existingOpt.isPresent()) {
                Invoice existing = existingOpt.get();
                if (existing.getStatus() == InvoiceStatus.PENDING
                        && existing.getPaymentMethod() == PaymentMethod.TRANSFER) {
                    return regenerateMomoLink(existing.getId());
                }
            }
        }
        // Lấy thông tin nhân viên thu ngân
        User cashier = userRepo.findById(cashierId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + cashierId));

        BigDecimal originalAmount = orderItemRepo.calculateOrderTotalAmount(orderId);

        // Xử lý voucher nếu có
        UserVoucher userVoucher = null;
        Voucher voucher = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (voucherCode != null && !voucherCode.isEmpty()) {
            userVoucher = userVoucherRepo.findByCode(voucherCode)
                    .orElseThrow(() -> new NoSuchElementException("Invalid voucher code: " + voucherCode));
            if (userVoucher.isUsed()) {
                throw new IllegalStateException("Voucher already used");
            }
            if (userVoucher.getExpiryAt().isBefore(LocalDateTime.now())) {
                throw new IllegalStateException("Voucher expired");
            }
            voucher = userVoucher.getVoucher();
            if (voucher.isActive()) {
                switch (voucher.getType()) {
                    case PERCENTAGE_DISCOUNT:
                        if (originalAmount.compareTo(voucher.getMinOrderAmount()) >= 0) {
                            BigDecimal percent = voucher.getDiscountValue()
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                            discountAmount = originalAmount.multiply(percent).setScale(2, RoundingMode.HALF_UP);
                        }
                        break;
                    case FIXED_DISCOUNT:
                        if (originalAmount.compareTo(voucher.getMinOrderAmount()) >= 0) {
                            discountAmount = voucher.getDiscountValue();
                        }
                        break;
                    case BUY_ONE_GET_ONE:
                        Optional<OrderItem> firstItemOpt = order.getOrderItems().stream().findFirst();
                        if (firstItemOpt.isPresent() && firstItemOpt.get().getQuantity() >= 2) {
                            discountAmount = firstItemOpt.get().getMenuItem().getPrice();
                        }
                        break;
                }
                if (discountAmount.compareTo(originalAmount) > 0) {
                    discountAmount = originalAmount;
                }
                userVoucher.setUsed(true);
                userVoucherRepo.save(userVoucher);
            } else {
                throw new IllegalStateException("Voucher is inactive");
            }
        }

        // Tính thành tiền cuối cùng
        BigDecimal totalAmount = originalAmount.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);

        // Xây dựng hóa đơn
        Invoice invoice = Invoice.builder()
                .order(order)
                .voucher(voucher)
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .cashier(cashier)
                .paymentTime(LocalDateTime.now())
                .paymentMethod(paymentMethod)
                .status(
                        paymentMethod == PaymentMethod.CASH ? InvoiceStatus.PAID
                                : (paymentMethod == PaymentMethod.TRANSFER ? InvoiceStatus.PENDING
                                        : InvoiceStatus.PAID))
                .build();
        String payUrl = null;
        String deeplink = null;
        String deeplinkQr = null;

        // Với chuyển khoản: tạo link MoMo trước, nếu fail -> throw để rollback
        if (paymentMethod == PaymentMethod.TRANSFER) {
            try {
                // generate unique momoOrderId per attempt
                UUID momoOrderId = UUID.randomUUID();
                var resp = momoClient.createPaymentLink(totalAmount.toPlainString(), momoOrderId.toString(),
                        "Payment for order #" + orderId);
                Object payUrlObj = resp.get("payUrl");
                Object deeplinkObj = resp.get("deeplink");
                Object deeplinkQrObj = resp.get("deeplinkQr");
                payUrl = payUrlObj != null ? String.valueOf(payUrlObj) : null;
                deeplink = deeplinkObj != null ? String.valueOf(deeplinkObj) : null;
                deeplinkQr = deeplinkQrObj != null ? String.valueOf(deeplinkQrObj) : null;
                invoice.setMomoOrderId(momoOrderId);
            } catch (Exception ex) {
                throw new IllegalStateException("Không thể tạo link MoMo: " + ex.getMessage(), ex);
            }
        }

        // Nếu thanh toán tiền mặt, hoàn tất luôn
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            order.setStatus(OrderStatus.PAID);
            if (order.getTable() != null) {
                order.getTable().setAvailable(true);
            }
            orderRepo.save(order);

            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                User customer = userRepo.findByPhoneNumber(phoneNumber)
                        .orElseThrow(() -> new NoSuchElementException("Customer not found: " + phoneNumber));
                int pointsEarned = totalAmount
                        .divide(BigDecimal.valueOf(1000), 0, RoundingMode.FLOOR)
                        .intValue();
                customer.setRewardPoints(
                        (customer.getRewardPoints() == null ? 0 : customer.getRewardPoints()) + pointsEarned);
                userRepo.save(customer);
            }

            revenueService.recordRevenue(
                    invoice.getPaymentTime().toLocalDate(),
                    paymentMethod,
                    invoice.getTotalAmount());
        }

        Invoice saved = invoiceRepo.save(invoice);
        return CreateInvoiceResponse.builder()
                .invoice(saved)
                .payUrl(payUrl)
                .deeplink(deeplink)
                .deeplinkQr(deeplinkQr)
                .build();
    }

    // Regenerate MoMo dynamic QR/link for a pending invoice
    public CreateInvoiceResponse regenerateMomoLink(Long invoiceId) {
        Invoice invoice = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found: " + invoiceId));
        if (invoice.getStatus() != InvoiceStatus.PENDING || invoice.getPaymentMethod() != PaymentMethod.TRANSFER) {
            throw new IllegalStateException(
                    "Chỉ có thể tạo lại link cho hóa đơn chuyển khoản đang ở trạng thái PENDING");
        }
        BigDecimal totalAmount = invoice.getTotalAmount();
        String orderInfo = "Payment for order #"
                + (invoice.getOrder() != null ? invoice.getOrder().getId() : invoice.getId());
        String payUrl = null, deeplink = null, deeplinkQr = null;
        try {
            UUID newMomoOrderId = UUID.randomUUID();
            var resp = momoClient.createPaymentLink(totalAmount.toPlainString(), newMomoOrderId.toString(), orderInfo);
            payUrl = resp.get("payUrl") != null ? String.valueOf(resp.get("payUrl")) : null;
            deeplink = resp.get("deeplink") != null ? String.valueOf(resp.get("deeplink")) : null;
            deeplinkQr = resp.get("deeplinkQr") != null ? String.valueOf(resp.get("deeplinkQr")) : null;
            invoice.setMomoOrderId(newMomoOrderId);
            invoiceRepo.save(invoice);
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể tạo lại link MoMo: " + ex.getMessage(), ex);
        }
        return CreateInvoiceResponse.builder()
                .invoice(invoice)
                .payUrl(payUrl)
                .deeplink(deeplink)
                .deeplinkQr(deeplinkQr)
                .build();
    }

    @Override
    public Optional<Invoice> getInvoiceById(Long id) {
        return invoiceRepo.findById(id);
    }

    @Override
    public Invoice findById(Long id) {
        return invoiceRepo.findById(id).orElseThrow(() -> new NoSuchElementException("Invoice not found: " + id));
    }

    @Override
    public Page<Invoice> getAllInvoice(Pageable pageable) {
        return invoiceRepo.findAll(pageable);
    }

    @Override
    public Page<Invoice> getInvoicesByDateRange(LocalDate start, LocalDate end, Pageable pageable) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt = end.plusDays(1).atStartOfDay().minusNanos(1);
        return invoiceRepo.findByPaymentTimeBetween(startDt, endDt, pageable);
    }

    @Override
    public InvoiceCalculationDTO calculateInvoiceAmount(Long orderId, String voucherCode) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        BigDecimal originalAmount = orderItemRepo.calculateOrderTotalAmount(orderId);

        AtomicReference<BigDecimal> discountAmount = new AtomicReference<>(BigDecimal.ZERO);

        if (voucherCode != null && !voucherCode.isEmpty()) {
            UserVoucher userVoucher = userVoucherRepo.findByCode(voucherCode)
                    .orElseThrow(() -> new NoSuchElementException("Mã voucher không tồn tại"));

            // Kiểm tra voucher đã được sử dụng
            if (userVoucher.isUsed()) {
                throw new IllegalStateException("Mã voucher này đã được sử dụng");
            }

            // Kiểm tra voucher đã hết hạn
            if (userVoucher.getExpiryAt().isBefore(LocalDateTime.now())) {
                throw new IllegalStateException("Mã voucher đã hết hạn");
            }

            Voucher voucher = userVoucher.getVoucher();

            // Kiểm tra voucher có active không
            if (!voucher.isActive()) {
                throw new IllegalStateException("Mã voucher không còn hoạt động");
            }

            // Kiểm tra đơn hàng đủ giá trị tối thiểu
            if (originalAmount.compareTo(voucher.getMinOrderAmount()) < 0) {
                throw new IllegalStateException(
                        String.format("Đơn hàng phải có giá trị tối thiểu %s VND để sử dụng voucher này",
                                voucher.getMinOrderAmount().toString()));
            }

            // Tính discount theo loại voucher
            switch (voucher.getType()) {
                case PERCENTAGE_DISCOUNT:
                    BigDecimal percent = voucher.getDiscountValue()
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    discountAmount.set(originalAmount.multiply(percent).setScale(2, RoundingMode.HALF_UP));
                    break;
                case FIXED_DISCOUNT:
                    discountAmount.set(voucher.getDiscountValue());
                    break;
                case BUY_ONE_GET_ONE:
                    order.getOrderItems().stream().findFirst().ifPresent(item -> {
                        if (item.getQuantity() >= 2) {
                            discountAmount.set(item.getMenuItem().getPrice());
                        }
                    });
                    break;
            }

            // Đảm bảo không vượt quá tiền gốc
            if (discountAmount.get().compareTo(originalAmount) > 0) {
                discountAmount.set(originalAmount);
            }
        }

        BigDecimal totalAmount = originalAmount.subtract(discountAmount.get()).setScale(2, RoundingMode.HALF_UP);
        return new InvoiceCalculationDTO(originalAmount, discountAmount.get(), totalAmount);
    }

    @Override
    public void markInvoicePaid(Long invoiceId) {
        Invoice invoice = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new NoSuchElementException("Invoice not found: " + invoiceId));
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return;
        }
        invoice.setStatus(InvoiceStatus.PAID);
        Order order = invoice.getOrder();
        if (order != null) {
            order.setStatus(OrderStatus.PAID);
            if (order.getTable() != null) {
                order.getTable().setAvailable(true);
            }
            orderRepo.save(order);
        }
        revenueService.recordRevenue(
                invoice.getPaymentTime().toLocalDate(),
                invoice.getPaymentMethod(),
                invoice.getTotalAmount());
        invoiceRepo.save(invoice);
    }

    @Override
    public Optional<Invoice> getInvoiceByOrderId(Long orderId) {
        return invoiceRepo.findByOrder_Id(orderId);
    }

    @Auditable(action = "DELETE_INVOICE", entityType = "INVOICE", description = "Delete invoice")
    public boolean deleteInvoice(Long id) {
        if (!invoiceRepo.existsById(id)) {
            return false;
        }
        Invoice invoice = invoiceRepo.findById(id).get();
        invoiceRepo.deleteById(id);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            revenueService.recordRevenue(
                    invoice.getPaymentTime().toLocalDate(),
                    invoice.getPaymentMethod(),
                    invoice.getTotalAmount().negate());
        }
        return true;
    }

    @Override
    public Map<String, Object> createInvoiceWithCardPayment(Long orderId, String voucherCode, Long cashierId,
            String cardType, String phoneNumber) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Validate card type
            if (!cardType.equalsIgnoreCase("ATM") && !cardType.equalsIgnoreCase("CREDIT")) {
                result.put("success", false);
                result.put("message", "Invalid card type. Must be 'ATM' or 'CREDIT'");
                return result;
            }

            // Lấy đơn hàng và kiểm tra trạng thái
            Order order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
            if (order.getStatus() != OrderStatus.SERVED) {
                result.put("success", false);
                result.put("message", "Only served orders can be paid");
                return result;
            }

            // Kiểm tra xem đã có invoice PENDING cho order này chưa
            Optional<Invoice> existingInvoiceOpt = invoiceRepo.findByOrder_Id(orderId);
            if (existingInvoiceOpt.isPresent()) {
                Invoice existing = existingInvoiceOpt.get();
                if (existing.getStatus() == InvoiceStatus.PENDING &&
                        existing.getPaymentMethod() == PaymentMethod.CARD) {
                    // Tạo lại link thanh toán cho invoice hiện tại
                    return regenerateCardPaymentLink(existing, cardType);
                } else if (existing.getStatus() == InvoiceStatus.PAID) {
                    result.put("success", false);
                    result.put("message", "Order already paid");
                    return result;
                }
            }

            // Lấy thông tin nhân viên thu ngân
            User cashier = userRepo.findById(cashierId)
                    .orElseThrow(() -> new NoSuchElementException("User not found: " + cashierId));

            // Tính toán số tiền
            InvoiceCalculationDTO calculation = calculateInvoiceAmount(orderId, voucherCode);

            // Xử lý voucher nếu có
            Voucher voucher = null;
            if (voucherCode != null && !voucherCode.isEmpty()) {
                UserVoucher userVoucher = userVoucherRepo.findByCode(voucherCode)
                        .orElseThrow(() -> new NoSuchElementException("Invalid voucher code: " + voucherCode));
                if (!userVoucher.isUsed() && userVoucher.getExpiryAt().isAfter(LocalDateTime.now())) {
                    voucher = userVoucher.getVoucher();
                    userVoucher.setUsed(true);
                    userVoucherRepo.save(userVoucher);
                }
            }

            // Tạo invoice với status PENDING (Instantiate Invoice object FIRST)
            Invoice invoice = Invoice.builder()
                    .order(order)
                    .voucher(voucher)
                    .originalAmount(calculation.getOriginalAmount())
                    .discountAmount(calculation.getDiscountAmount())
                    .totalAmount(calculation.getTotalAmount())
                    .cashier(cashier)
                    .paymentTime(LocalDateTime.now())
                    .paymentMethod(PaymentMethod.CARD)
                    .status(InvoiceStatus.PENDING)
                    // momoOrderId will be set later
                    .build();

            // Tạo MoMo payment link
            UUID momoOrderId = UUID.randomUUID();
            String orderInfo = "Card payment for order #" + orderId;
            Map<String, Object> momoResp;

            try {
                momoResp = momoClient.createCardPaymentLink(
                        calculation.getTotalAmount().toPlainString(),
                        momoOrderId.toString(),
                        orderInfo,
                        cardType);

                // Kiểm tra response từ MoMo
                Object resultCode = momoResp.get("resultCode");
                if (resultCode != null && !"0".equals(String.valueOf(resultCode))) {
                    result.put("success", false);
                    result.put("message", "Failed to create MoMo payment link");
                    result.put("momoResponse", momoResp);
                    return result;
                }
            } catch (Exception ex) {
                result.put("success", false);
                result.put("message", "Error creating MoMo payment link: " + ex.getMessage());
                return result;
            }

            // Set momoOrderId vào invoice
            invoice.setMomoOrderId(momoOrderId);

            // Save invoice
            Invoice savedInvoice = invoiceRepo.save(invoice);

            // Tạo response
            result.put("success", true);
            result.put("message", "Invoice created successfully with card payment");
            result.put("invoice", savedInvoice);
            result.put("payUrl", momoResp.get("payUrl"));
            result.put("deeplink", momoResp.get("deeplink"));
            result.put("qrCodeUrl", momoResp.get("qrCodeUrl"));
            result.put("cardType", cardType);
            result.put("momoOrderId", momoOrderId.toString());

            return result;

        } catch (NoSuchElementException ex) {
            result.put("success", false);
            result.put("message", ex.getMessage());
            return result;
        } catch (Exception ex) {
            result.put("success", false);
            result.put("message", "Error creating invoice: " + ex.getMessage());
            return result;
        }
    }

    /**
     * Helper method để tạo lại link thanh toán thẻ cho invoice hiện tại
     */
    private Map<String, Object> regenerateCardPaymentLink(Invoice invoice, String cardType) {
        Map<String, Object> result = new HashMap<>();
        try {
            UUID newMomoOrderId = UUID.randomUUID();
            String orderInfo = "Card payment for order #" +
                    (invoice.getOrder() != null ? invoice.getOrder().getId() : invoice.getId());

            Map<String, Object> momoResp = momoClient.createCardPaymentLink(
                    invoice.getTotalAmount().toPlainString(),
                    newMomoOrderId.toString(),
                    orderInfo,
                    cardType);

            // Cập nhật momoOrderId mới
            invoice.setMomoOrderId(newMomoOrderId);
            invoiceRepo.save(invoice);

            result.put("success", true);
            result.put("message", "Payment link regenerated successfully");
            result.put("invoice", invoice);
            result.put("payUrl", momoResp.get("payUrl"));
            result.put("deeplink", momoResp.get("deeplink"));
            result.put("qrCodeUrl", momoResp.get("qrCodeUrl"));
            result.put("cardType", cardType);
            result.put("momoOrderId", newMomoOrderId.toString());

            return result;
        } catch (Exception ex) {
            result.put("success", false);
            result.put("message", "Error regenerating payment link: " + ex.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> processMomoWebhook(Map<String, String> ipnData) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Extract IPN data
            String orderId = ipnData.get("orderId");
            String resultCode = ipnData.get("resultCode");
            String transId = ipnData.get("transId");
            String amount = ipnData.get("amount");

            System.out.println("Processing MoMo IPN: orderId=" + orderId +
                    " resultCode=" + resultCode +
                    " transId=" + transId +
                    " amount=" + amount);

            // Xác thực signature
            boolean isValidSignature = momoClient.verifyIpnSignature(ipnData);
            if (!isValidSignature) {
                System.err.println("Invalid MoMo IPN signature for orderId: " + orderId);
                result.put("status", "error");
                result.put("message", "Invalid signature");
                return result;
            }

            // Kiểm tra resultCode
            if (resultCode == null || orderId == null) {
                result.put("status", "error");
                result.put("message", "Missing required parameters");
                return result;
            }

            // Tìm invoice theo momoOrderId
            Invoice invoice = null;
            try {
                UUID momoOrderUuid = UUID.fromString(orderId);
                invoice = invoiceRepo.findByMomoOrderId(momoOrderUuid).orElse(null);
            } catch (IllegalArgumentException ex) {
                // orderId không phải UUID, thử parse long
                try {
                    Long orderIdLong = Long.parseLong(orderId);
                    invoice = getInvoiceByOrderId(orderIdLong).orElse(null);
                } catch (NumberFormatException nfe) {
                    System.err.println("Cannot parse orderId: " + orderId);
                }
            }

            if (invoice == null) {
                System.err.println("Invoice not found for MoMo orderId: " + orderId);
                result.put("status", "error");
                result.put("message", "Invoice not found");
                return result;
            }

            // Kiểm tra nếu đã paid rồi thì chỉ return ack (idempotent)
            if (invoice.getStatus() == InvoiceStatus.PAID) {
                System.out.println("Invoice already paid: " + invoice.getId());
                result.put("status", "success");
                result.put("message", "Already processed");
                result.put("invoiceId", invoice.getId());
                return result;
            }

            // Xử lý theo resultCode
            if ("0".equals(resultCode)) {
                // Thanh toán thành công
                System.out.println("Payment successful for invoice: " + invoice.getId());
                markInvoicePaid(invoice.getId());

                result.put("status", "success");
                result.put("message", "Payment confirmed");
                result.put("invoiceId", invoice.getId());
                result.put("orderId", invoice.getOrder() != null ? invoice.getOrder().getId() : null);
                return result;
            } else {
                // Thanh toán thất bại
                System.out.println("Payment failed for invoice: " + invoice.getId() +
                        " resultCode: " + resultCode +
                        " message: " + ipnData.get("message"));

                result.put("status", "success");
                result.put("message", "Payment failure acknowledged");
                result.put("invoiceId", invoice.getId());
                return result;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            result.put("status", "error");
            result.put("message", "Error processing IPN: " + ex.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> createInvoiceWithPayPal(Long orderId, String voucherCode, Long cashierId,
            String phoneNumber) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Lấy đơn hàng và kiểm tra trạng thái
            Order order = orderRepo.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
            if (order.getStatus() != OrderStatus.SERVED) {
                result.put("success", false);
                result.put("message", "Only served orders can be paid");
                return result;
            }

            // Kiểm tra xem đã có invoice PENDING cho order này chưa
            Optional<Invoice> existingInvoiceOpt = invoiceRepo.findByOrder_Id(orderId);
            if (existingInvoiceOpt.isPresent()) {
                Invoice existing = existingInvoiceOpt.get();
                if (existing.getStatus() == InvoiceStatus.PENDING &&
                        existing.getPaymentMethod() == PaymentMethod.CARD && existing.getPaypalOrderId() != null) {
                    // Nếu đã có invoice PayPal pending, trả về link thanh toán cũ hoặc tạo mới
                    // Ở đây ta tạo mới link cho đơn giản
                    // return regeneratePayPalLink(existing);
                } else if (existing.getStatus() == InvoiceStatus.PAID) {
                    result.put("success", false);
                    result.put("message", "Order already paid");
                    return result;
                }
            }

            // Lấy thông tin nhân viên thu ngân
            User cashier = userRepo.findById(cashierId)
                    .orElseThrow(() -> new NoSuchElementException("User not found: " + cashierId));

            // Tính toán số tiền
            InvoiceCalculationDTO calculation = calculateInvoiceAmount(orderId, voucherCode);

            // Xử lý voucher nếu có
            Voucher voucher = null;
            if (voucherCode != null && !voucherCode.isEmpty()) {
                UserVoucher userVoucher = userVoucherRepo.findByCode(voucherCode)
                        .orElseThrow(() -> new NoSuchElementException("Invalid voucher code: " + voucherCode));
                if (!userVoucher.isUsed() && userVoucher.getExpiryAt().isAfter(LocalDateTime.now())) {
                    voucher = userVoucher.getVoucher();
                    userVoucher.setUsed(true);
                    userVoucherRepo.save(userVoucher);
                }
            }

            // Tạo invoice với status PENDING
            Invoice invoice = Invoice.builder()
                    .order(order)
                    .voucher(voucher)
                    .originalAmount(calculation.getOriginalAmount())
                    .discountAmount(calculation.getDiscountAmount())
                    .totalAmount(calculation.getTotalAmount())
                    .cashier(cashier)
                    .paymentTime(LocalDateTime.now())
                    .paymentMethod(PaymentMethod.CARD)
                    .status(InvoiceStatus.PENDING)
                    .build();

            // Tạo PayPal order
            String orderInfo = "Payment for order #" + orderId;
            Map<String, Object> paypalResp = payPalClient.createCardPaymentOrder(
                    calculation.getTotalAmount().toPlainString(),
                    String.valueOf(orderId),
                    orderInfo);

            if (!(boolean) paypalResp.get("success")) {
                result.put("success", false);
                result.put("message", paypalResp.get("message"));
                return result;
            }

            String paypalOrderId = (String) paypalResp.get("paypalOrderId");
            invoice.setPaypalOrderId(paypalOrderId);

            // Save invoice
            Invoice savedInvoice = invoiceRepo.save(invoice);

            result.put("success", true);
            result.put("message", "Invoice created successfully with PayPal");
            result.put("invoice", savedInvoice);
            result.put("approvalUrl", paypalResp.get("approvalUrl"));
            result.put("paypalOrderId", paypalOrderId);

            return result;

        } catch (Exception ex) {
            result.put("success", false);
            result.put("message", "Error creating invoice: " + ex.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> processPayPalWebhook(Map<String, Object> webhookData) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Verify signature (skip for now or implement if needed)
            // boolean isValid = payPalClient.verifyWebhookSignature(webhookData);

            String eventType = (String) webhookData.get("event_type");
            Map<String, Object> resource = (Map<String, Object>) webhookData.get("resource");

            if (resource == null) {
                result.put("status", "error");
                result.put("message", "Missing resource data");
                return result;
            }

            String paypalOrderId = (String) resource.get("id");

            // Handle CHECKOUT.ORDER.APPROVED or PAYMENT.CAPTURE.COMPLETED
            if ("CHECKOUT.ORDER.APPROVED".equals(eventType)) {
                // Capture payment
                Map<String, Object> captureResult = payPalClient.capturePaymentOrder(paypalOrderId);
                if ((boolean) captureResult.get("success")) {
                    // Payment captured, update invoice
                    Invoice invoice = invoiceRepo.findByPaypalOrderId(paypalOrderId).orElse(null);
                    if (invoice != null) {
                        markInvoicePaid(invoice.getId());
                        result.put("status", "success");
                        result.put("message", "Payment captured and invoice paid");
                        result.put("orderId", invoice.getOrder().getId());
                    } else {
                        result.put("status", "error");
                        result.put("message", "Invoice not found for PayPal order: " + paypalOrderId);
                    }
                } else {
                    result.put("status", "error");
                    result.put("message", "Failed to capture payment");
                }
            } else if ("PAYMENT.CAPTURE.COMPLETED".equals(eventType)) {
                // Just update invoice if not already paid
                // Note: resource.id here might be capture ID, not order ID.
                // Need to check PayPal API structure. Usually supplemental_data contains
                // order_id
                // For simplicity, we rely on CHECKOUT.ORDER.APPROVED to capture and mark paid.
                result.put("status", "success");
                result.put("message", "Event received");
            } else {
                result.put("status", "ignored");
                result.put("message", "Event type not handled: " + eventType);
            }

            return result;

        } catch (Exception ex) {
            ex.printStackTrace();
            result.put("status", "error");
            result.put("message", "Error processing webhook: " + ex.getMessage());
            return result;
        }
    }
}
