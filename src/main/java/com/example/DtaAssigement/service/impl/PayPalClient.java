package com.example.DtaAssigement.service.impl;

import com.example.DtaAssigement.config.PayPalProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PayPalClient {

    private static final Logger logger = LoggerFactory.getLogger(PayPalClient.class);

    private final PayPalProperties props;
    private final ObjectMapper objectMapper;
    @Qualifier("restTemplate")
    private final RestTemplate restTemplate;
    private PayPalHttpClient client;

    /**
     * Initialize PayPal HTTP client with environment
     */
    private PayPalHttpClient getClient() {
        if (client == null) {
            PayPalEnvironment environment;
            if ("live".equalsIgnoreCase(props.getMode())) {
                environment = new PayPalEnvironment.Live(props.getClientId(), props.getClientSecret());
            } else {
                environment = new PayPalEnvironment.Sandbox(props.getClientId(), props.getClientSecret());
            }
            client = new PayPalHttpClient(environment);
            logger.info("PayPal client initialized in {} mode", props.getMode());
        }
        return client;
    }

    /**
     * Create PayPal order for card payment
     * 
     * @param amount    Amount to charge (in VND, will be converted to USD)
     * @param orderId   Order ID for tracking
     * @param orderInfo Description of the order
     * @return Map containing order details including approval URL
     */
    public Map<String, Object> createCardPaymentOrder(String amount, String orderId, String orderInfo) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Convert VND to USD using configured exchange rate
            BigDecimal amountVnd = new BigDecimal(amount);
            BigDecimal amountUsd = amountVnd.divide(props.getExchangeRateVndToUsd(), 2, BigDecimal.ROUND_HALF_UP);

            logger.info("Creating PayPal order - orderId: {}, amount: {} VND ({} USD)",
                    orderId, amount, amountUsd.toPlainString());

            // Build order request
            OrderRequest orderRequest = new OrderRequest();
            orderRequest.checkoutPaymentIntent("CAPTURE");

            // Application context (return URLs)
            ApplicationContext applicationContext = new ApplicationContext()
                    .returnUrl(props.getReturnSuccessUrl())
                    .cancelUrl(props.getReturnCancelUrl())
                    .brandName("Cafe Management System")
                    .landingPage("BILLING")
                    .shippingPreference("NO_SHIPPING")
                    .userAction("PAY_NOW");
            orderRequest.applicationContext(applicationContext);

            // Purchase unit
            List<PurchaseUnitRequest> purchaseUnits = new ArrayList<>();
            PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                    .referenceId(orderId)
                    .description(orderInfo != null ? orderInfo : ("Payment for order " + orderId))
                    .customId(orderId)
                    .softDescriptor("CAFE ORDER")
                    .amountWithBreakdown(new AmountWithBreakdown()
                            .currencyCode(props.getCurrency())
                            .value(amountUsd.toPlainString()));

            purchaseUnits.add(purchaseUnit);
            orderRequest.purchaseUnits(purchaseUnits);

            // Create order request
            OrdersCreateRequest request = new OrdersCreateRequest();
            request.prefer("return=representation");
            request.requestBody(orderRequest);

            // Execute request
            HttpResponse<Order> response = getClient().execute(request);
            Order order = response.result();

            logger.info("PayPal order created successfully - PayPal Order ID: {}, Status: {}",
                    order.id(), order.status());

            // Extract approval URL
            String approvalUrl = null;
            for (LinkDescription link : order.links()) {
                if ("approve".equals(link.rel())) {
                    approvalUrl = link.href();
                    break;
                }
            }

            result.put("success", true);
            result.put("paypalOrderId", order.id());
            result.put("status", order.status());
            result.put("approvalUrl", approvalUrl);
            result.put("orderId", orderId);

            return result;

        } catch (IOException e) {
            logger.error("Error creating PayPal order for orderId: {}", orderId, e);
            result.put("success", false);
            result.put("message", "Failed to create PayPal order: " + e.getMessage());
            return result;
        }
    }

    /**
     * Capture payment for an approved order
     * 
     * @param paypalOrderId PayPal order ID to capture
     * @return Map containing capture details
     */
    public Map<String, Object> capturePaymentOrder(String paypalOrderId) {
        Map<String, Object> result = new HashMap<>();

        try {
            logger.info("Capturing PayPal payment - PayPal Order ID: {}", paypalOrderId);

            OrdersCaptureRequest request = new OrdersCaptureRequest(paypalOrderId);
            request.prefer("return=representation");

            HttpResponse<Order> response = getClient().execute(request);
            Order order = response.result();

            logger.info("PayPal payment captured successfully - PayPal Order ID: {}, Status: {}",
                    order.id(), order.status());

            result.put("success", true);
            result.put("paypalOrderId", order.id());
            result.put("status", order.status());
            result.put("order", order);

            return result;

        } catch (IOException e) {
            logger.error("Error capturing PayPal payment for order: {}", paypalOrderId, e);
            result.put("success", false);
            result.put("message", "Failed to capture PayPal payment: " + e.getMessage());
            return result;
        }
    }

    /**
     * Get order details
     * 
     * @param paypalOrderId PayPal order ID
     * @return Map containing order details
     */
    public Map<String, Object> getOrderDetails(String paypalOrderId) {
        Map<String, Object> result = new HashMap<>();

        try {
            OrdersGetRequest request = new OrdersGetRequest(paypalOrderId);
            HttpResponse<Order> response = getClient().execute(request);
            Order order = response.result();

            result.put("success", true);
            result.put("order", order);
            result.put("status", order.status());

            return result;

        } catch (IOException e) {
            logger.error("Error getting PayPal order details: {}", paypalOrderId, e);
            result.put("success", false);
            result.put("message", "Failed to get order details: " + e.getMessage());
            return result;
        }
    }

    /**
     * Verify webhook signature to ensure the webhook event actually came from PayPal.
     * Calls PayPal's /v2/notifications/verify-webhook-signature REST API via RestTemplate
     * with Basic Auth (client credentials).
     *
     * @param webhookEvent        Raw webhook event data (already parsed from JSON body)
     * @param transmissionId     PayPal-Transmission-Id header
     * @param transmissionTime   PayPal-Transmission-Time header
     * @param certUrl            PayPal-Cert-Url header
     * @param authAlgo           PayPal-Auth-Algo header
     * @param transmissionSig    PayPal-Transmission-Sig header
     * @return true if signature is valid, false otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean verifyWebhookSignature(
            Map<String, Object> webhookEvent,
            String transmissionId,
            String transmissionTime,
            String certUrl,
            String authAlgo,
            String transmissionSig) {

        if (props.getLoggingEnabled()) {
            logger.info("Verifying PayPal webhook signature - transmissionId: {}", transmissionId);
        }

        try {
            String webhookId = props.getWebhookId();
            if (webhookId == null || webhookId.isEmpty()) {
                logger.warn("PayPal webhook-id is not configured, skipping verification");
                return false;
            }

            // Step 1: Get access token using client credentials (Basic Auth)
            String tokenUrl = props.getApiBaseUrl() + "/v1/oauth2/token";
            HttpHeaders tokenHeaders = new HttpHeaders();
            tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            tokenHeaders.setBasicAuth(props.getClientId(), props.getClientSecret());

            HttpEntity<String> tokenEntity = new HttpEntity<>(String.format(
                    "grant_type=client_credentials&ignoreCache=true&return_authn_schemes=false&return_client_metadata=true&return_unconsented_scopes=true"),
                    tokenHeaders);

            var tokenResp = restTemplate.exchange(tokenUrl, org.springframework.http.HttpMethod.POST,
                    tokenEntity, Map.class);
            Map<String, Object> tokenBody = tokenResp.getBody();
            String accessToken = tokenBody != null ? (String) tokenBody.get("access_token") : null;

            if (accessToken == null) {
                logger.error("Failed to obtain PayPal access token");
                return false;
            }

            // Step 2: Verify webhook signature
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("auth_algo", authAlgo);
            requestBody.put("cert_url", certUrl);
            requestBody.put("transmission_id", transmissionId);
            requestBody.put("transmission_sig", transmissionSig);
            requestBody.put("transmission_time", transmissionTime);
            requestBody.put("webhook_id", webhookId);
            requestBody.put("webhook_event", webhookEvent);

            String jsonBody = objectMapper.writeValueAsString(requestBody);
            String verifyUrl = props.getApiBaseUrl() + "/v1/notifications/verify-webhook-signature";

            logger.info("PayPal Webhook Verification Request Body: {}", jsonBody);
            logger.info("Headers - transmissionId: {}, transmissionTime: {}, authAlgo: {}, certUrl: {}, transmissionSig: {}",
                    transmissionId, transmissionTime, authAlgo, certUrl, transmissionSig);

            HttpHeaders verifyHeaders = new HttpHeaders();
            verifyHeaders.setContentType(MediaType.APPLICATION_JSON);
            verifyHeaders.setBearerAuth(accessToken);
            verifyHeaders.set("PayPal-Request-Id", java.util.UUID.randomUUID().toString());

            HttpEntity<String> verifyEntity = new HttpEntity<>(jsonBody, verifyHeaders);
            var verifyResp = restTemplate.exchange(verifyUrl, org.springframework.http.HttpMethod.POST,
                    verifyEntity, Map.class);
            Map<String, Object> verifyBody = verifyResp.getBody();

            logger.info("PayPal Webhook Verification Response: {}", verifyBody);

            boolean isValid = verifyBody != null && "SUCCESS".equals(verifyBody.get("verification_status"));
            logger.info("Webhook signature verification result: {} (status={})",
                    isValid ? "VALID" : "INVALID", verifyBody != null ? verifyBody.get("verification_status") : "null");
            return isValid;

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize webhook event for verification", e);
            return false;
        } catch (Exception e) {
            logger.error("Error verifying PayPal webhook signature", e);
            return false;
        }
    }
}
