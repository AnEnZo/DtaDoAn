package com.example.DtaAssigement.controller;

import com.example.DtaAssigement.dto.InvoiceCalculationDTO;
import com.example.DtaAssigement.dto.CreateInvoiceResponse;
import com.example.DtaAssigement.ennum.OrderStatus;
import com.example.DtaAssigement.ennum.PaymentMethod;
import com.example.DtaAssigement.ennum.InvoiceStatus;
import com.example.DtaAssigement.entity.Invoice;
import com.example.DtaAssigement.entity.Order;
import com.example.DtaAssigement.entity.User;
import com.example.DtaAssigement.entity.UserVoucher;
import com.example.DtaAssigement.entity.Voucher;
import com.example.DtaAssigement.repository.OrderRepository;
import com.example.DtaAssigement.repository.InvoiceRepository;
import com.example.DtaAssigement.repository.UserRepository;
import com.example.DtaAssigement.repository.UserVoucherRepository;
import com.example.DtaAssigement.service.InvoiceService;
import com.example.DtaAssigement.service.impl.MomoClient;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invoices")
@SecurityRequirement(name = "bearerAuth")
@AllArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final OrderRepository orderRepo;
    private final InvoiceRepository invoiceRepo;
    private final UserRepository userRepo;
    private final UserVoucherRepository userVoucherRepo;
    private final MomoClient momoClient;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','USER')")
    public Page<Invoice> getAllInvoice(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paymentTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        System.out.println("Request pageable: " + pageable);
        Page<Invoice> pageResult = invoiceService.getAllInvoice(pageable);
        System.out.println("Respond pagination: totalElements=" + pageResult.getTotalElements() +
                ", contentSize=" + pageResult.getContent().size());
        return pageResult;
    }

    // MoMo redirect bridge: render a self-contained result page for the customer's
    // browser (works directly from the public host, no frontend/localhost dependency).
    // NOTE: this page is display-only UX. The authoritative "paid" status is set by the
    // verified IPN webhook (/webhook/momo), not by these query params.
    @GetMapping(value = "/return/momo", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("permitAll()")
    public void momoReturn(HttpServletResponse response, @RequestParam Map<String, String> params) throws IOException {
        boolean success = "0".equals(params.getOrDefault("resultCode", ""));
        String title = success ? "Thanh toán thành công!" : "Thanh toán không thành công";
        String subtitle = success
                ? "Cảm ơn bạn. Giao dịch đã được ghi nhận."
                : "Giao dịch chưa hoàn tất. Vui lòng thử lại hoặc liên hệ nhân viên.";

        Map<String, String> details = new java.util.LinkedHashMap<>();
        details.put("Nội dung", params.get("orderInfo"));
        details.put("Số tiền", formatVndAmount(params.get("amount")));
        details.put("Trạng thái", params.get("message"));

        writeHtml(response, buildResultPage(success, title, subtitle, details));
    }

    /** Write a UTF-8 HTML page to the response. */
    private void writeHtml(HttpServletResponse response, String html) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(html);
    }

    /** Format a raw VND amount string like "130000" into "130.000₫"; returns input on failure. */
    private String formatVndAmount(String amount) {
        if (amount == null || amount.isBlank()) return amount;
        try {
            long amt = new BigDecimal(amount).longValueExact();
            return String.format("%,d", amt).replace(',', '.') + "₫";
        } catch (Exception ignored) {
            return amount;
        }
    }

    /**
     * Build a small, self-contained payment-result page shared by MoMo & PayPal.
     * All dynamic values are HTML-escaped (reflected-XSS safe). {@code details} is an
     * ordered label->value map; entries with blank values are skipped.
     */
    private String buildResultPage(boolean success, String title, String subtitle, Map<String, String> details) {
        String accent = success ? "#16a34a" : "#dc2626";
        String bgFrom = success ? "#0f9d58" : "#c0392b";
        String bgTo = success ? "#16a34a" : "#e74c3c";
        String icon = success ? "✓" : "✕";

        StringBuilder rows = new StringBuilder();
        if (details != null) {
            for (Map.Entry<String, String> e : details.entrySet()) {
                if (e.getValue() == null || e.getValue().isBlank()) continue;
                rows.append("<div class=\"row\"><span>").append(escapeHtml(e.getKey()))
                        .append("</span><b>").append(escapeHtml(e.getValue())).append("</b></div>");
            }
        }

        return "<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + escapeHtml(title) + "</title><style>"
                + "*{box-sizing:border-box;margin:0;padding:0}"
                + "body{font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;background:linear-gradient(135deg," + bgFrom + "," + bgTo + ");"
                + "min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px;color:#1f2937}"
                + ".card{background:#fff;border-radius:20px;box-shadow:0 20px 50px rgba(0,0,0,.25);max-width:380px;width:100%;padding:32px 28px;text-align:center}"
                + ".badge{width:84px;height:84px;border-radius:50%;margin:0 auto 20px;display:flex;align-items:center;justify-content:center;"
                + "font-size:44px;color:#fff;background:" + accent + "}"
                + "h1{font-size:1.35rem;margin-bottom:8px;color:" + accent + "}"
                + "p.sub{color:#6b7280;font-size:.95rem;margin-bottom:20px}"
                + ".details{background:#f9fafb;border-radius:12px;padding:14px 16px;text-align:left}"
                + ".row{display:flex;justify-content:space-between;gap:12px;padding:6px 0;font-size:.9rem}"
                + ".row span{color:#6b7280}.row b{color:#111827;text-align:right;word-break:break-word}"
                + ".note{margin-top:18px;font-size:.8rem;color:#9ca3af}"
                + "</style></head><body><div class=\"card\">"
                + "<div class=\"badge\">" + icon + "</div>"
                + "<h1>" + escapeHtml(title) + "</h1>"
                + "<p class=\"sub\">" + escapeHtml(subtitle) + "</p>"
                + (rows.length() > 0 ? "<div class=\"details\">" + rows + "</div>" : "")
                + "<p class=\"note\">Bạn có thể đóng cửa sổ này và quay lại quầy.</p>"
                + "</div></body></html>";
    }

    /** Minimal HTML escaping to prevent reflected XSS from query parameters. */
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','USER')")
    public Page<Invoice> getInvoicesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "paymentTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return invoiceService.getInvoicesByDateRange(start, end, pageable);
    }

    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','USER')")
    public ResponseEntity<Invoice> getInvoiceById(@PathVariable Long invoiceId) {
        return invoiceService.getInvoiceById(invoiceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Tạo hóa đơn cho đơn hàng đã phục vụ
    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<?> createInvoice(@RequestParam Long orderId,
            @RequestParam(required = false) String voucherCode,
            @RequestParam Long cashierId,
            @RequestParam PaymentMethod paymentMethod,
            @RequestParam(required = false) String phoneNumber) {
        // IllegalState -> 409, NoSuchElement -> 404, IllegalArgument -> 400 (handled globally)
        CreateInvoiceResponse created = invoiceService.createInvoice(orderId, voucherCode, cashierId, paymentMethod,
                phoneNumber);
        return ResponseEntity.ok(created);
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deleteInvoice(@RequestParam Long invoiceId) {
        // DataIntegrityViolationException -> 409 (handled globally)
        boolean deleted = invoiceService.deleteInvoice(invoiceId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        throw new NoSuchElementException("hóa đơn để xóa");
    }

    @GetMapping(value = "/{orderId}/qrcode")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<?> getQRCode(@PathVariable Long orderId) {
        Order order = orderRepo.readById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        BigDecimal total = order.getOrderItems().stream()
                .map(item -> item.getMenuItem().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        String accountNumber = "0564870803";
        String accountName = "DINH TUAN AN";
        String acqId = "970422"; // Mã ngân hàng MB Bank
        String addInfo = "Thanh toan don hang #" + orderId;

        try {
            // Gọi VietQR API
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("accountNo", accountNumber);
            body.put("accountName", accountName);
            body.put("acqId", acqId);
            body.put("amount", total);
            body.put("addInfo", addInfo);
            body.put("template", "print");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.vietqr.io/v2/generate",
                    request,
                    Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                String qrDataURL = (String) data.get("qrDataURL");
                // qrDataURL có dạng "data:image/png;base64,AAAAB3NzaC1yc2E..."

                // Tách và decode phần Base64
                String base64 = qrDataURL.substring(qrDataURL.indexOf(',') + 1);
                byte[] imageBytes = java.util.Base64.getDecoder().decode(base64);

                // Đọc BufferedImage từ bytes
                ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
                BufferedImage qrImage = ImageIO.read(bis);
                bis.close();

                // Ghi ra byte[] để trả về
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(qrImage, "PNG", baos);

                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(baos.toByteArray());
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể tạo mã QR từ VietQR");
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error generating QR code for order {}", orderId, e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Lỗi khi tạo mã QR thanh toán");
        }
    }

    @GetMapping("/calculate")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','USER')")
    public ResponseEntity<?> calculateInvoice(
            @RequestParam Long orderId,
            @RequestParam(required = false) String voucherCode) {
        // IllegalState -> 409, NoSuchElement -> 404 (voucher/order errors handled globally)
        InvoiceCalculationDTO dto = invoiceService.calculateInvoiceAmount(orderId, voucherCode);
        return ResponseEntity.ok(dto);
    }

    /**
     * MoMo Webhook/IPN (Instant Payment Notification) Callback
     * Endpoint này sẽ được MoMo gọi khi thanh toán hoàn tất
     * Bao gồm xác thực signature để đảm bảo request đến từ MoMo
     * 
     * @param ipnData Dữ liệu từ MoMo
     * @return Response xác nhận đã nhận IPN
     */
    @PostMapping("/webhook/momo")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> momoWebhook(@RequestBody Map<String, String> ipnData) {
        Map<String, Object> result = invoiceService.processMomoWebhook(ipnData);

        // Notify SSE subscribers if payment was successful
        if ("success".equals(result.get("status")) &&
                "Payment confirmed".equals(result.get("message"))) {
            Long orderId = (Long) result.get("orderId");
            if (orderId != null) {
                CopyOnWriteArrayList<SseEmitter> list = emitters.get(orderId);
                if (list != null) {
                    list.forEach(em -> {
                        try {
                            em.send(SseEmitter.event().name("paid").data("PAID"));
                            em.complete();
                        } catch (Exception ignored) {
                        }
                    });
                    list.clear();
                }
            }
        }

        return ResponseEntity.ok(result);
    }

    // SSE subscribe by orderId
    @GetMapping(value = "/sse/order/{orderId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("permitAll()")
    public SseEmitter subscribeOrder(@PathVariable Long orderId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(orderId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> emitters.getOrDefault(orderId, new CopyOnWriteArrayList<>()).remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.getOrDefault(orderId, new CopyOnWriteArrayList<>()).remove(emitter);
        });
        return emitter;
    }

    // Create MoMo dynamic link/QR for an order
    @PostMapping("/{orderId}/momo-link")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<?> createMomoLink(@PathVariable Long orderId) {
        Order order = orderRepo.readById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        BigDecimal total = order.getOrderItems().stream()
                .map(item -> item.getMenuItem().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        try {
            Map<String, Object> resp = momoClient.createPaymentLink(total.toPlainString(), String.valueOf(orderId),
                    "Payment for order #" + orderId);
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            log.error("Error creating MoMo payment link for order {}", orderId, ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Không thể tạo link thanh toán MoMo");
        }
    }

    /**
     * Tạo hóa đơn với thanh toán thẻ MoMo (ATM/Visa/Mastercard)
     * API này sẽ tạo invoice và trả về link thanh toán qua thẻ
     * 
     * @param orderId     ID đơn hàng
     * @param voucherCode Mã voucher (optional)
     * @param cashierId   ID nhân viên thu ngân
     * @param cardType    Loại thẻ: "ATM" hoặc "CREDIT"
     * @param phoneNumber Số điện thoại khách hàng (optional)
     * @return Response chứa invoice và payment URL
     */
    @PostMapping("/create-with-card-payment")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<?> createInvoiceWithCardPayment(
            @RequestParam Long orderId,
            @RequestParam(required = false) String voucherCode,
            @RequestParam Long cashierId,
            @RequestParam(defaultValue = "ATM") String cardType,
            @RequestParam(required = false) String phoneNumber) {

        Map<String, Object> result = invoiceService.createInvoiceWithCardPayment(
                orderId, voucherCode, cashierId, cardType, phoneNumber);

        boolean success = (boolean) result.getOrDefault("success", false);
        if (success) {
            return ResponseEntity.ok(result);
        } else {
            String message = (String) result.get("message");
            if (message != null && message.contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Tạo hóa đơn với thanh toán PayPal
     * API này sẽ tạo invoice và trả về link thanh toán PayPal
     */
    @PostMapping("/create-with-paypal")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<?> createInvoiceWithPayPal(
            @RequestParam Long orderId,
            @RequestParam(required = false) String voucherCode,
            @RequestParam Long cashierId,
            @RequestParam(required = false) String phoneNumber) {

        Map<String, Object> result = invoiceService.createInvoiceWithPayPal(
                orderId, voucherCode, cashierId, phoneNumber);

        boolean success = (boolean) result.getOrDefault("success", false);
        if (success) {
            return ResponseEntity.ok(result);
        } else {
            String message = (String) result.get("message");
            if (message != null && message.contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
            }
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * PayPal Webhook Callback
     */
    @PostMapping("/paypal/webhook")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> paypalWebhook(
            @RequestBody Map<String, Object> webhookData,
            @RequestHeader(value = "PayPal-Transmission-Id", required = false) String transmissionId,
            @RequestHeader(value = "PayPal-Transmission-Time", required = false) String transmissionTime,
            @RequestHeader(value = "PayPal-Cert-Url", required = false) String certUrl,
            @RequestHeader(value = "PayPal-Auth-Algo", required = false) String authAlgo,
            @RequestHeader(value = "PayPal-Transmission-Sig", required = false) String transmissionSig) {
        Map<String, Object> result = invoiceService.processPayPalWebhook(
                webhookData, transmissionId, transmissionTime, certUrl, authAlgo, transmissionSig);

        // Notify SSE subscribers if payment was successful
        if ("success".equals(result.get("status"))) {
            Long orderId = (Long) result.get("orderId");
            if (orderId != null) {
                CopyOnWriteArrayList<SseEmitter> list = emitters.get(orderId);
                if (list != null) {
                    list.forEach(em -> {
                        try {
                            em.send(SseEmitter.event().name("paid").data("PAID"));
                            em.complete();
                        } catch (Exception ignored) {
                        }
                    });
                    list.clear();
                }
            }
        }

        return ResponseEntity.ok(result);
    }

    // PayPal redirect bridge: success — render shared result page
    @GetMapping(value = "/paypal/success", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("permitAll()")
    public void paypalSuccess(HttpServletResponse response, @RequestParam Map<String, String> params)
            throws IOException {
        Map<String, String> details = new java.util.LinkedHashMap<>();
        details.put("Phương thức", "PayPal");
        writeHtml(response, buildResultPage(true,
                "Thanh toán thành công!",
                "Cảm ơn bạn. Giao dịch PayPal đã được ghi nhận.",
                details));
    }

    // PayPal redirect bridge: cancel — render shared result page
    @GetMapping(value = "/paypal/cancel", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("permitAll()")
    public void paypalCancel(HttpServletResponse response) throws IOException {
        Map<String, String> details = new java.util.LinkedHashMap<>();
        details.put("Phương thức", "PayPal");
        writeHtml(response, buildResultPage(false,
                "Đã hủy thanh toán",
                "Bạn đã hủy giao dịch PayPal. Đơn hàng vẫn chưa được thanh toán.",
                details));
    }

}