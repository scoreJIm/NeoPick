package com.neopick.adapter.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neopick.domain.booking.Booking;
import com.neopick.domain.payment.Payment;
import com.neopick.infrastructure.config.NeopickProperties;
import com.neopick.port.payment.PaymentGateway;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WeChat Pay V3 gateway adapter implementing the PaymentGateway port.
 * Uses the Native API to return a QR code URL, and verifies callbacks via
 * RSA signature verification plus AES-256-GCM resource decryption.
 */
@Component
public class WechatPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(WechatPaymentGateway.class);

    private static final String API_BASE_URL = "https://api.mch.weixin.qq.com";
    private static final String NATIVE_PAY_PATH = "/v3/pay/transactions/native";
    private static final String QUERY_PATH = "/v3/pay/transactions/out-trade-no/";
    private static final String REFUND_PATH = "/v3/refund/domestic/refunds";
    private static final String CURRENCY = "CNY";
    private static final String TRADE_STATE_SUCCESS = "SUCCESS";

    public static final String CALLBACK_BODY_KEY = "body";
    public static final String CALLBACK_SIGNATURE_KEY = WechatSignatureUtils.HEADER_SIGNATURE;
    public static final String CALLBACK_TIMESTAMP_KEY = WechatSignatureUtils.HEADER_TIMESTAMP;
    public static final String CALLBACK_NONCE_KEY = WechatSignatureUtils.HEADER_NONCE;
    public static final String CALLBACK_SERIAL_KEY = WechatSignatureUtils.HEADER_SERIAL;

    private final NeopickProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WechatPaymentGateway(NeopickProperties properties,
                                RestTemplate wechatRestTemplate,
                                ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = wechatRestTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "initiatePaymentFallback")
    public InitiatePaymentResult initiatePayment(Payment payment, Booking booking) {
        NeopickProperties.PaymentProperties.WechatProperties cfg = properties.payment().wechat();

        String outTradeNo = payment.getId().value().toString();
        String description = "Guitar Lesson - Booking " + booking.getId().value().toString().substring(0, 8);
        long totalCents = toCents(payment.getAmount());

        Map<String, Object> amount = new LinkedHashMap<>();
        amount.put("total", totalCents);
        amount.put("currency", CURRENCY);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appid", cfg.appId());
        body.put("mchid", cfg.mchId());
        body.put("description", description);
        body.put("out_trade_no", outTradeNo);
        body.put("notify_url", cfg.notifyUrl());
        body.put("amount", amount);

        String json = writeJson(body);
        String url = API_BASE_URL + NATIVE_PAY_PATH;
        HttpHeaders headers = buildHeaders("POST", NATIVE_PAY_PATH, json, cfg);

        String response = restTemplate.postForObject(url, new HttpEntity<>(json, headers), String.class);
        String codeUrl = extractCodeUrl(response);

        log.info("WeChat Pay initiated: outTradeNo={}, amount={}", outTradeNo, payment.getAmount());
        return new InitiatePaymentResult(codeUrl != null, outTradeNo, codeUrl, codeUrl);
    }

    @SuppressWarnings("unused")
    InitiatePaymentResult initiatePaymentFallback(Payment payment, Booking booking, Throwable t) {
        log.error("Payment gateway circuit breaker open — payment {} failed", payment.getId().value(), t);
        throw new RuntimeException("Payment gateway is temporarily unavailable. Please try again later.", t);
    }

    @Override
    public CallbackResult verifyCallback(Map<String, String> params) {
        NeopickProperties.PaymentProperties.WechatProperties cfg = properties.payment().wechat();

        String body = params.get(CALLBACK_BODY_KEY);
        String signature = params.get(CALLBACK_SIGNATURE_KEY);
        String timestamp = params.get(CALLBACK_TIMESTAMP_KEY);
        String nonce = params.get(CALLBACK_NONCE_KEY);
        String serial = params.get(CALLBACK_SERIAL_KEY);

        if (isBlank(body) || isBlank(signature) || isBlank(timestamp) || isBlank(nonce)) {
            log.warn("WeChat callback missing required signature fields");
            return new CallbackResult(false, false, null, null, null, null);
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(WechatSignatureUtils.HEADER_SIGNATURE, signature);
        headers.put(WechatSignatureUtils.HEADER_TIMESTAMP, timestamp);
        headers.put(WechatSignatureUtils.HEADER_NONCE, nonce);
        headers.put(WechatSignatureUtils.HEADER_SERIAL, serial);

        if (!WechatSignatureUtils.verifySignature(headers, body, cfg.platformCert())) {
            log.warn("WeChat callback signature verification failed");
            return new CallbackResult(false, false, null, null, null, null);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> notification = objectMapper.readValue(body, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> resource = (Map<String, Object>) notification.get("resource");
            String ciphertext = (String) resource.get("ciphertext");
            String resourceNonce = (String) resource.get("nonce");
            String associatedData = (String) resource.get("associated_data");

            String plaintext = WechatSignatureUtils.decryptResource(
                    ciphertext, resourceNonce, associatedData, cfg.apiV3Key());

            @SuppressWarnings("unchecked")
            Map<String, Object> transaction = objectMapper.readValue(plaintext, Map.class);

            String outTradeNo = (String) transaction.get("out_trade_no");
            String transactionId = (String) transaction.get("transaction_id");
            String tradeState = (String) transaction.get("trade_state");
            BigDecimal totalAmount = extractAmount(transaction);

            boolean success = TRADE_STATE_SUCCESS.equals(tradeState);
            log.info("WeChat callback verified: outTradeNo={}, transactionId={}, state={}, success={}",
                    outTradeNo, transactionId, tradeState, success);

            return new CallbackResult(success, true, transactionId, outTradeNo, totalAmount, tradeState);
        } catch (Exception e) {
            log.warn("WeChat callback decryption or parsing failed", e);
            return new CallbackResult(false, true, null, null, null, null);
        }
    }

    @Override
    public QueryResult queryPayment(String outTradeNo) {
        NeopickProperties.PaymentProperties.WechatProperties cfg = properties.payment().wechat();

        String path = QUERY_PATH + outTradeNo + "?mchid=" + cfg.mchId();
        String url = API_BASE_URL + path;
        HttpHeaders headers = buildHeaders("GET", path, "", cfg);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> root = objectMapper.readValue(response.getBody(), Map.class);

            String transactionId = (String) root.get("transaction_id");
            String tradeState = (String) root.get("trade_state");
            BigDecimal totalAmount = extractAmount(root);

            log.debug("WeChat query response for {}: state={}", outTradeNo, tradeState);
            return new QueryResult(tradeState != null, transactionId, outTradeNo, totalAmount, tradeState);
        } catch (Exception e) {
            log.error("WeChat query error for {}: {}", outTradeNo, e.getMessage(), e);
            return new QueryResult(false, null, outTradeNo, null, e.getMessage());
        }
    }

    @Override
    public RefundResult refundPayment(String outTradeNo, BigDecimal amount, String reason) {
        NeopickProperties.PaymentProperties.WechatProperties cfg = properties.payment().wechat();

        String outRefundNo = UUID.randomUUID().toString();
        long refundCents = toCents(amount);

        // WeChat requires the original order total; resolve it from the transaction when possible.
        long totalCents = refundCents;
        QueryResult query = queryPayment(outTradeNo);
        if (query.success() && query.totalAmount() != null) {
            totalCents = toCents(query.totalAmount());
        }

        Map<String, Object> amountBody = new LinkedHashMap<>();
        amountBody.put("refund", refundCents);
        amountBody.put("total", totalCents);
        amountBody.put("currency", CURRENCY);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("out_trade_no", outTradeNo);
        body.put("out_refund_no", outRefundNo);
        body.put("reason", reason);
        body.put("amount", amountBody);

        String json = writeJson(body);
        String url = API_BASE_URL + REFUND_PATH;
        HttpHeaders headers = buildHeaders("POST", REFUND_PATH, json, cfg);

        try {
            String response = restTemplate.postForObject(url, new HttpEntity<>(json, headers), String.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> root = objectMapper.readValue(response, Map.class);
            String status = (String) root.get("status");
            String refundId = (String) root.get("refund_id");
            boolean success = "SUCCESS".equals(status) || "PROCESSING".equals(status);

            log.info("WeChat refund for {}: status={}, refundId={}", outTradeNo, status, refundId);
            return new RefundResult(success, refundId, outTradeNo, amount);
        } catch (Exception e) {
            log.error("WeChat refund error for {}: {}", outTradeNo, e.getMessage(), e);
            return new RefundResult(false, null, outTradeNo, amount);
        }
    }

    @Override
    public String supportedMethod() {
        return "WECHAT";
    }

    private HttpHeaders buildHeaders(String method, String path, String body,
                                     NeopickProperties.PaymentProperties.WechatProperties cfg) {
        String authorization = WechatSignatureUtils.buildAuthorizationHeader(
                method, path, body, cfg.mchId(), cfg.serialNo(), cfg.privateKey());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize WeChat Pay request", e);
        }
    }

    private String extractCodeUrl(String response) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = objectMapper.readValue(response, Map.class);
            return (String) root.get("code_url");
        } catch (Exception e) {
            log.warn("Failed to parse WeChat native pay response: {}", response, e);
            return null;
        }
    }

    private BigDecimal extractAmount(Map<String, Object> payload) {
        Object amount = payload.get("amount");
        if (amount instanceof Map<?, ?> amountMap) {
            Object total = amountMap.get("total");
            if (total instanceof Number number) {
                return BigDecimal.valueOf(number.longValue(), 2);
            }
        }
        return null;
    }

    private long toCents(BigDecimal yuan) {
        return yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }
}
