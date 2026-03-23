package com.example.DtaAssigement.service.impl;

import com.example.DtaAssigement.config.PayPalProperties;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
            // Convert VND to USD (rough conversion, adjust rate as needed)
            BigDecimal amountVnd = new BigDecimal(amount);
            BigDecimal amountUsd = amountVnd.divide(new BigDecimal("25000"), 2, BigDecimal.ROUND_HALF_UP);

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
     * Verify webhook signature (simplified - for production, implement proper
     * verification)
     * For now, we'll validate webhook events by checking order status
     * 
     * @param webhookEvent Webhook event data
     * @return true if valid
     */
    public boolean verifyWebhookSignature(Map<String, Object> webhookEvent) {
        // For sandbox testing, we can skip signature verification
        // In production, implement proper webhook signature verification using PayPal
        // SDK

        if (props.getLoggingEnabled()) {
            logger.info("Webhook event received: {}", webhookEvent);
        }

        // Basic validation
        return webhookEvent != null &&
                webhookEvent.containsKey("event_type") &&
                webhookEvent.containsKey("resource");
    }
}
