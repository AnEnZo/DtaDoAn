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

    // MoMo redirect bridge: redirect browser to frontend dashboard#orders
    @GetMapping("/return/momo")
    @PreAuthorize("permitAll()")
    public void momoReturn(HttpServletResponse response, @RequestParam Map<String, String> params) throws IOException {
        String query = params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        // Frontend base (dev). Change to your deployed frontend if needed.
        String frontendBase = "http://localhost:5173";
        String target = frontendBase + "/dashboard#orders" + (query.isEmpty() ? "" : ("?" + query));
        response.sendRedirect(target);
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
        try {
            CreateInvoiceResponse created = invoiceService.createInvoice(orderId, voucherCode, cashierId, paymentMethod,
                    phoneNumber);
            return ResponseEntity.ok(created);
        } catch (IllegalStateException | NoSuchElementException | IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tạo hóa đơn: " + ex.getMessage());
        }
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deleteInvoice(@RequestParam Long invoiceId) {
        try {
            boolean deleted = invoiceService.deleteInvoice(invoiceId);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy hóa đơn để xóa.");
            }
        } catch (Exception e) {
            e.printStackTrace(); // In lỗi cụ thể ra console/log
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi xóa hóa đơn: " + e.getMessage());
        }
    }

    @GetMapping(value = "/{orderId}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
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
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Không thể tạo QR từ VietQR API");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi trong quá trình tạo QR Code: " + e.getMessage());
        }
    }

    @GetMapping("/calculate")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN','USER')")
    public ResponseEntity<?> calculateInvoice(
            @RequestParam Long orderId,
            @RequestParam(required = false) String voucherCode) {
        try {
            InvoiceCalculationDTO dto = invoiceService.calculateInvoiceAmount(orderId, voucherCode);
            return ResponseEntity.ok(dto);
        } catch (IllegalStateException | NoSuchElementException ex) {
            // Return clean error message for voucher errors
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tính toán hóa đơn: " + ex.getMessage());
        }
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
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

    // PayPal redirect bridge: success
    @GetMapping("/paypal/success")
    @PreAuthorize("permitAll()")
    public void paypalSuccess(HttpServletResponse response, @RequestParam Map<String, String> params)
            throws IOException {
        // Redirect to frontend dashboard
        String frontendBase = "http://localhost:5173";
        String target = frontendBase + "/dashboard#orders?paypal_status=success";
        response.sendRedirect(target);
    }

    // PayPal redirect bridge: cancel
    @GetMapping("/paypal/cancel")
    @PreAuthorize("permitAll()")
    public void paypalCancel(HttpServletResponse response) throws IOException {
        // Redirect to frontend dashboard
        String frontendBase = "http://localhost:5173";
        String target = frontendBase + "/dashboard#orders?paypal_status=cancel";
        response.sendRedirect(target);
    }

}