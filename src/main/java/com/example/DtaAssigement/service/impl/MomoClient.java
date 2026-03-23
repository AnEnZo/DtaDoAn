package com.example.DtaAssigement.service.impl;

import com.example.DtaAssigement.config.MomoProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Data
public class MomoClient {

    private static final Logger logger = LoggerFactory.getLogger(MomoClient.class);

    private final MomoProperties props;
    private final RestTemplate restTemplate;

    /**
     * Create payment link / dynamic QR — trả Map chứa response body + txId.
     * Sử dụng MoMo Payment Gateway API để tạo payment link và QR code
     */
    public Map<String, Object> createPaymentLink(String amount, String orderId, String orderInfo) {
        // Validate input parameters
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount cannot be null or empty");
        }
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }

        String path = "/v2/gateway/api/create";
        String requestId = UUID.randomUUID().toString();
        // MoMo expects integer amount (VND) without decimals
        long amountLong;
        try {
            java.math.BigDecimal bd = new java.math.BigDecimal(amount);
            amountLong = bd.setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid amount format: " + amount);
        }
        String amountForSig = Long.toString(amountLong);
        String signature = generateSignature(amountForSig, orderId, orderInfo, requestId);

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("partnerCode", props.getPartnerCode());
        body.put("requestType", props.getRequestType()); // e.g., captureWallet
        body.put("ipnUrl", props.getIpnUrl());
        body.put("redirectUrl", props.getRedirectUrl());
        body.put("orderId", orderId);
        body.put("amount", amountLong); // numeric JSON
        body.put("lang", "vi");
        body.put("orderInfo", orderInfo != null ? orderInfo : ("Payment for order " + orderId));
        body.put("requestId", requestId);
        body.put("extraData", "");
        body.put("signature", signature);
        logger.info("createPaymentLink -> path={} requestId={} orderId={} amount={}", path, requestId, orderId, amount);
        logger.debug("createPaymentLink -> signature generated successfully");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = props.getBaseUrl() + path;
        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                String bodyStr = String.valueOf(resp.getBody());
                logger.error("createPaymentLink failed -> status={} body={}", resp.getStatusCode(),
                        truncate(bodyStr, 2000));
                throw new IllegalStateException(
                        "Create payment link failed: " + resp.getStatusCode() + " body:" + bodyStr);
            }
            Map<String, Object> respMap = resp.getBody();
            Map<String, Object> result = new HashMap<>(respMap != null ? respMap : Map.of());
            result.put("requestId", requestId);
            logger.debug("createPaymentLink -> success requestId={} respKeys={}", requestId, result.keySet());
            return result;
        } catch (RestClientException ex) {
            logger.error("createPaymentLink exception -> requestId={} msg={}", requestId, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Generate signature for MoMo Payment Gateway API
     * Signature string format:
     * accessKey={accessKey}&amount={amount}&extraData={extraData}&ipnUrl={ipnUrl}&orderId={orderId}&orderInfo={orderInfo}&partnerCode={partnerCode}&redirectUrl={redirectUrl}&requestId={requestId}&requestType={requestType}
     */
    private String generateSignature(String amount, String orderId, String orderInfo, String requestId) {
        try {
            // Tạo chuỗi signature theo thứ tự yêu cầu của MoMo
            String rawSignature = "accessKey=" + props.getAccessKey() +
                    "&amount=" + amount +
                    "&extraData=" + "" +
                    "&ipnUrl=" + props.getIpnUrl() +
                    "&orderId=" + orderId +
                    "&orderInfo=" + orderInfo +
                    "&partnerCode=" + props.getPartnerCode() +
                    "&redirectUrl=" + props.getRedirectUrl() +
                    "&requestId=" + requestId +
                    "&requestType=" + props.getRequestType();

            logger.debug("Raw signature string: {}", rawSignature);

            // Tạo HMAC SHA256 signature
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(props.getSecretKey().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] bytes = mac.doFinal(rawSignature.getBytes(StandardCharsets.UTF_8));
            String signature = bytesToHex(bytes);

            logger.debug("Generated signature: {}", signature);
            return signature;
        } catch (Exception e) {
            logger.error("Error generating signature: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating signature", e);
        }
    }

    // small helper to avoid huge logs
    private static String truncate(String s, int max) {
        if (s == null)
            return null;
        return s.length() <= max ? s : s.substring(0, max) + "...(truncated)";
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * Create card payment link - Thanh toán qua thẻ ATM/Visa/Mastercard
     * Request type phải là payWithATM hoặc payWithCC
     * 
     * @param amount    Số tiền thanh toán
     * @param orderId   Mã đơn hàng
     * @param orderInfo Thông tin đơn hàng
     * @param cardType  Loại thẻ: "ATM" hoặc "CREDIT" (Visa/Mastercard)
     * @return Map chứa response từ MoMo (payUrl, qrCodeUrl, deeplink, etc.)
     */
    public Map<String, Object> createCardPaymentLink(String amount, String orderId, String orderInfo, String cardType) {
        // Validate input parameters
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount cannot be null or empty");
        }
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        if (cardType == null || (!cardType.equalsIgnoreCase("ATM") && !cardType.equalsIgnoreCase("CREDIT"))) {
            throw new IllegalArgumentException("Card type must be either 'ATM' or 'CREDIT'");
        }

        String path = "/v2/gateway/api/create";
        String requestId = UUID.randomUUID().toString();

        // Determine request type based on card type
        String requestType = cardType.equalsIgnoreCase("ATM") ? "payWithATM" : "payWithCC";

        // MoMo expects integer amount (VND) without decimals
        long amountLong;
        try {
            java.math.BigDecimal bd = new java.math.BigDecimal(amount);
            amountLong = bd.setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid amount format: " + amount);
        }
        String amountForSig = Long.toString(amountLong);

        // Generate signature with specific request type
        String signature = generateCardPaymentSignature(amountForSig, orderId, orderInfo, requestId, requestType);

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("partnerCode", props.getPartnerCode());
        body.put("partnerName", props.getPartnerName()); // Required for payWithATM/payWithCC
        body.put("storeId", props.getStoreId()); // Required for payWithATM/payWithCC
        body.put("requestType", requestType);

        // Add userInfo (Required for payWithATM/payWithCC)
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("email", props.getUserInfoEmail()); // Use configured email
        body.put("userInfo", userInfo);

        body.put("ipnUrl", props.getIpnUrl());
        body.put("redirectUrl", props.getRedirectUrl());
        body.put("orderId", orderId);
        body.put("amount", amountLong);
        body.put("lang", "vi");
        body.put("orderInfo", orderInfo != null ? orderInfo : ("Card payment for order " + orderId));
        body.put("requestId", requestId);
        body.put("extraData", "");
        body.put("signature", signature);

        // ===== DEBUG LOGGING START =====
        logger.info("==================================================");
        logger.info("MOMO CARD PAYMENT REQUEST - DEBUG");
        logger.info("==================================================");
        logger.info("Card Type: {}", cardType);
        logger.info("Request Type: {}", requestType);
        logger.info("Partner Code: {}", props.getPartnerCode());
        logger.info("Order ID: {}", orderId);
        logger.info("Amount: {}", amountLong);
        logger.info("Order Info: {}", orderInfo);
        logger.info("Request ID: {}", requestId);
        logger.info("IPN URL: {}", props.getIpnUrl());
        logger.info("Redirect URL: {}", props.getRedirectUrl());
        logger.info("Extra Data: (empty string)");
        logger.info("Generated Signature: {}", signature);
        logger.info("Full Request Body: {}", body);
        logger.info("==================================================");
        // ===== DEBUG LOGGING END =====

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = props.getBaseUrl() + path;

        logger.info("Sending POST request to: {}", url);

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);

            logger.info("==================================================");
            logger.info("MOMO CARD PAYMENT RESPONSE");
            logger.info("==================================================");
            logger.info("HTTP Status: {}", resp.getStatusCode());
            logger.info("Response Body: {}", resp.getBody());
            logger.info("==================================================");

            if (!resp.getStatusCode().is2xxSuccessful()) {
                String bodyStr = String.valueOf(resp.getBody());
                logger.error("createCardPaymentLink failed -> status={} body={}",
                        resp.getStatusCode(), truncate(bodyStr, 2000));
                throw new IllegalStateException("Create card payment link failed: " +
                        resp.getStatusCode() + " body:" + bodyStr);
            }
            Map<String, Object> respMap = resp.getBody();
            Map<String, Object> result = new HashMap<>(respMap != null ? respMap : Map.of());
            result.put("requestId", requestId);
            logger.info("createCardPaymentLink -> success requestId={} resultCode={}",
                    requestId, result.get("resultCode"));
            return result;
        } catch (RestClientException ex) {
            logger.error("createCardPaymentLink exception -> requestId={} msg={}",
                    requestId, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Generate signature for card payment
     */
    private String generateCardPaymentSignature(String amount, String orderId, String orderInfo,
            String requestId, String requestType) {
        try {
            String rawSignature = "accessKey=" + props.getAccessKey() +
                    "&amount=" + amount +
                    "&extraData=" + "" +
                    "&ipnUrl=" + props.getIpnUrl() +
                    "&orderId=" + orderId +
                    "&orderInfo=" + orderInfo +
                    "&partnerCode=" + props.getPartnerCode() +
                    "&redirectUrl=" + props.getRedirectUrl() +
                    "&requestId=" + requestId +
                    "&requestType=" + requestType;

            logger.info("==================================================");
            logger.info("SIGNATURE GENERATION - CARD PAYMENT");
            logger.info("==================================================");
            logger.info("Access Key: {}", props.getAccessKey());
            logger.info("Amount: {}", amount);
            logger.info("Extra Data: (empty)");
            logger.info("IPN URL: {}", props.getIpnUrl());
            logger.info("Order ID: {}", orderId);
            logger.info("Order Info: {}", orderInfo);
            logger.info("Partner Code: {}", props.getPartnerCode());
            logger.info("Redirect URL: {}", props.getRedirectUrl());
            logger.info("Request ID: {}", requestId);
            logger.info("Request Type: {}", requestType);
            logger.info("--------------------------------------------------");
            logger.info("Raw Signature String: {}", rawSignature);
            logger.info("Secret Key (first 10 chars): {}...",
                    props.getSecretKey().substring(0, Math.min(10, props.getSecretKey().length())));
            logger.info("==================================================");

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(props.getSecretKey().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] bytes = mac.doFinal(rawSignature.getBytes(StandardCharsets.UTF_8));
            String signature = bytesToHex(bytes);

            logger.debug("Generated card payment signature: {}", signature);
            return signature;
        } catch (Exception e) {
            logger.error("Error generating card payment signature: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating card payment signature", e);
        }
    }

    /**
     * Verify IPN (Instant Payment Notification) signature from MoMo
     * Sử dụng để xác thực callback từ MoMo về server
     * 
     * @param ipnData Map chứa dữ liệu IPN từ MoMo
     * @return true nếu signature hợp lệ, false nếu không
     */
    public boolean verifyIpnSignature(Map<String, String> ipnData) {
        try {
            String signature = ipnData.get("signature");
            if (signature == null || signature.isEmpty()) {
                logger.warn("verifyIpnSignature -> signature is missing");
                return false;
            }

            // Tạo lại signature từ dữ liệu IPN
            String rawSignature = "accessKey=" + props.getAccessKey() +
                    "&amount=" + ipnData.get("amount") +
                    "&extraData=" + (ipnData.get("extraData") != null ? ipnData.get("extraData") : "") +
                    "&message=" + ipnData.get("message") +
                    "&orderId=" + ipnData.get("orderId") +
                    "&orderInfo=" + ipnData.get("orderInfo") +
                    "&orderType=" + ipnData.get("orderType") +
                    "&partnerCode=" + ipnData.get("partnerCode") +
                    "&payType=" + ipnData.get("payType") +
                    "&requestId=" + ipnData.get("requestId") +
                    "&responseTime=" + ipnData.get("responseTime") +
                    "&resultCode=" + ipnData.get("resultCode") +
                    "&transId=" + ipnData.get("transId");

            logger.debug("verifyIpnSignature -> raw signature string: {}", rawSignature);

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(props.getSecretKey().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] bytes = mac.doFinal(rawSignature.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = bytesToHex(bytes);

            boolean isValid = calculatedSignature.equals(signature);
            logger.info("verifyIpnSignature -> orderId={} isValid={}", ipnData.get("orderId"), isValid);

            if (!isValid) {
                logger.warn("verifyIpnSignature -> signature mismatch. Expected: {}, Received: {}",
                        calculatedSignature, signature);
            }

            return isValid;
        } catch (Exception e) {
            logger.error("verifyIpnSignature -> error: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Query transaction status - Kiểm tra trạng thái giao dịch
     * 
     * @param orderId   Mã đơn hàng cần kiểm tra
     * @param requestId Request ID gốc khi tạo payment
     * @return Map chứa thông tin trạng thái giao dịch
     */
    public Map<String, Object> queryTransactionStatus(String orderId, String requestId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }

        String path = "/v2/gateway/api/query";

        // Generate signature for query
        String rawSignature = "accessKey=" + props.getAccessKey() +
                "&orderId=" + orderId +
                "&partnerCode=" + props.getPartnerCode() +
                "&requestId=" + requestId;

        String signature;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(props.getSecretKey().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] bytes = mac.doFinal(rawSignature.getBytes(StandardCharsets.UTF_8));
            signature = bytesToHex(bytes);
        } catch (Exception e) {
            logger.error("queryTransactionStatus -> signature generation error: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating query signature", e);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("partnerCode", props.getPartnerCode());
        body.put("requestId", requestId);
        body.put("orderId", orderId);
        body.put("lang", "vi");
        body.put("signature", signature);

        logger.info("queryTransactionStatus -> orderId={} requestId={}", orderId, requestId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = props.getBaseUrl() + path;

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> result = resp.getBody();
            if (result != null) {
                logger.info("queryTransactionStatus -> orderId={} resultCode={} transId={}",
                        orderId, result.get("resultCode"), result.get("transId"));
            }
            return result != null ? result : new HashMap<>();
        } catch (RestClientException ex) {
            logger.error("queryTransactionStatus exception -> orderId={} msg={}",
                    orderId, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Refund transaction - Hoàn tiền cho giao dịch
     * 
     * @param transId     Transaction ID từ MoMo (nhận được sau thanh toán thành
     *                    công)
     * @param amount      Số tiền cần hoàn (phải <= số tiền gốc)
     * @param description Mô tả lý do hoàn tiền
     * @return Map chứa kết quả hoàn tiền
     */
    public Map<String, Object> refundTransaction(String transId, String amount, String description) {
        if (transId == null || transId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount cannot be null or empty");
        }

        String path = "/v2/gateway/api/refund";
        String requestId = UUID.randomUUID().toString();

        // Convert amount to long
        long amountLong;
        try {
            java.math.BigDecimal bd = new java.math.BigDecimal(amount);
            amountLong = bd.setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid amount format: " + amount);
        }

        // Generate signature for refund
        String rawSignature = "accessKey=" + props.getAccessKey() +
                "&amount=" + amountLong +
                "&description=" + (description != null ? description : "") +
                "&orderId=" + requestId +
                "&partnerCode=" + props.getPartnerCode() +
                "&requestId=" + requestId +
                "&transId=" + transId;

        String signature;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(props.getSecretKey().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] bytes = mac.doFinal(rawSignature.getBytes(StandardCharsets.UTF_8));
            signature = bytesToHex(bytes);
        } catch (Exception e) {
            logger.error("refundTransaction -> signature generation error: {}", e.getMessage(), e);
            throw new RuntimeException("Error generating refund signature", e);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("partnerCode", props.getPartnerCode());
        body.put("requestId", requestId);
        body.put("orderId", requestId); // Use requestId as orderId for refund
        body.put("amount", amountLong);
        body.put("transId", transId);
        body.put("lang", "vi");
        body.put("description", description != null ? description : "Refund for transaction " + transId);
        body.put("signature", signature);

        logger.info("refundTransaction -> transId={} amount={} requestId={}", transId, amount, requestId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = props.getBaseUrl() + path;

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> result = resp.getBody();
            if (result != null) {
                logger.info("refundTransaction -> transId={} resultCode={}",
                        transId, result.get("resultCode"));
            }
            return result != null ? result : new HashMap<>();
        } catch (RestClientException ex) {
            logger.error("refundTransaction exception -> transId={} msg={}",
                    transId, ex.getMessage(), ex);
            throw ex;
        }
    }

}
