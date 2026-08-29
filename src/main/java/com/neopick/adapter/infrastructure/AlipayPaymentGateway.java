package com.neopick.adapter.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neopick.domain.booking.Booking;
import com.neopick.domain.payment.Payment;
import com.neopick.infrastructure.config.NeopickProperties;
import com.neopick.port.payment.PaymentGateway;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Alipay sandbox payment gateway adapter implementing the PaymentGateway port.
 * Uses direct HTTP with RSA2 signing instead of the Alipay SDK for a minimal footprint.
 */
@Component
@Primary
public class AlipayPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(AlipayPaymentGateway.class);
    private static final DateTimeFormatter ALIPAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PAGE_PAY_METHOD = "alipay.trade.page.pay";
    private static final String QUERY_METHOD = "alipay.trade.query";
    private static final String REFUND_METHOD = "alipay.trade.refund";
    private static final String FORMAT = "JSON";
    private static final String CHARSET = "utf-8";
    private static final String VERSION = "1.0";
    private static final String PRODUCT_CODE = "FAST_INSTANT_TRADE_PAY";

    private final NeopickProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AlipayPaymentGateway(NeopickProperties properties,
                                RestTemplate alipayRestTemplate,
                                ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = alipayRestTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "initiatePaymentFallback")
    public InitiatePaymentResult initiatePayment(Payment payment, Booking booking) {
        NeopickProperties.PaymentProperties.AlipayProperties cfg = properties.payment().alipay();

        String subject = "Guitar Lesson - Booking " + booking.getId().value().toString().substring(0, 8);
        String body = "Lesson scheduled for " + booking.getScheduledStart();

        Map<String, String> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", payment.getId().value().toString());
        bizContent.put("product_code", PRODUCT_CODE);
        bizContent.put("total_amount", payment.getAmount().toString());
        bizContent.put("subject", subject);
        bizContent.put("body", body);

        Map<String, String> params = buildBaseParams(cfg, PAGE_PAY_METHOD);
        params.put("notify_url", cfg.notifyUrl());
        params.put("return_url", cfg.returnUrl());
        try {
            params.put("biz_content", objectMapper.writeValueAsString(bizContent));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize biz_content", e);
        }

        String sign = AlipaySignatureUtils.sign(params, cfg.privateKey());
        params.put("sign", sign);

        String payUrl = cfg.gatewayUrl() + "?" + buildQueryString(params);

        log.info("Alipay payment initiated: outTradeNo={}, amount={}", payment.getId().value(), payment.getAmount());
        return new InitiatePaymentResult(true, payment.getId().value().toString(), payUrl, null);
    }

    @SuppressWarnings("unused")
    InitiatePaymentResult initiatePaymentFallback(Payment payment, Booking booking, Throwable t) {
        log.error("Payment gateway circuit breaker open — payment {} failed", payment.getId().value(), t);
        throw new RuntimeException("Payment gateway is temporarily unavailable. Please try again later.", t);
    }

    @Override
    public CallbackResult verifyCallback(Map<String, String> params) {
        NeopickProperties.PaymentProperties.AlipayProperties cfg = properties.payment().alipay();
        String sign = params.get("sign");

        if (sign == null || sign.isEmpty()) {
            log.warn("Alipay callback missing signature");
            return new CallbackResult(false, false, null, null, null, null);
        }

        boolean valid = AlipaySignatureUtils.verify(params, sign, cfg.alipayPublicKey());
        if (!valid) {
            log.warn("Alipay callback signature verification failed");
            return new CallbackResult(false, false, null, null, null, null);
        }

        String tradeNo = params.get("trade_no");
        String outTradeNo = params.get("out_trade_no");
        BigDecimal totalAmount = new BigDecimal(params.getOrDefault("total_amount", "0"));
        String tradeStatus = params.get("trade_status");

        boolean success = "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);
        log.info("Alipay callback verified: outTradeNo={}, tradeNo={}, status={}, success={}",
                outTradeNo, tradeNo, tradeStatus, success);

        return new CallbackResult(success, true, tradeNo, outTradeNo, totalAmount, tradeStatus);
    }

    @Override
    public QueryResult queryPayment(String outTradeNo) {
        NeopickProperties.PaymentProperties.AlipayProperties cfg = properties.payment().alipay();

        Map<String, String> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", outTradeNo);

        Map<String, String> params = buildBaseParams(cfg, QUERY_METHOD);
        try {
            params.put("biz_content", objectMapper.writeValueAsString(bizContent));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize biz_content", e);
        }

        String sign = AlipaySignatureUtils.sign(params, cfg.privateKey());
        params.put("sign", sign);

        try {
            String body = buildQueryString(params);
            String response = restTemplate.postForObject(cfg.gatewayUrl(), body, String.class);

            log.debug("Alipay query response for {}: {}", outTradeNo, response);

            @SuppressWarnings("unchecked")
            Map<String, Object> root = objectMapper.readValue(response, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = (Map<String, Object>)
                    ((Map<String, Object>) root.get("alipay_trade_query_response"));
            String code = (String) responseMap.get("code");
            if (!"10000".equals(code)) {
                log.warn("Alipay query failed: code={}, msg={}", code, responseMap.get("msg"));
                return new QueryResult(false, null, outTradeNo, null, null);
            }

            String tradeNo = (String) responseMap.get("trade_no");
            BigDecimal totalAmount = responseMap.get("total_amount") != null
                    ? new BigDecimal((String) responseMap.get("total_amount")) : null;
            String tradeStatus = (String) responseMap.get("trade_status");

            return new QueryResult(true, tradeNo, outTradeNo, totalAmount, tradeStatus);
        } catch (Exception e) {
            log.error("Alipay query error for {}: {}", outTradeNo, e.getMessage(), e);
            return new QueryResult(false, null, outTradeNo, null, e.getMessage());
        }
    }

    @Override
    public RefundResult refundPayment(String outTradeNo, BigDecimal amount, String reason) {
        NeopickProperties.PaymentProperties.AlipayProperties cfg = properties.payment().alipay();

        Map<String, String> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", outTradeNo);
        bizContent.put("refund_amount", amount.toString());
        bizContent.put("refund_reason", reason);
        bizContent.put("out_request_no", UUID.randomUUID().toString());

        Map<String, String> params = buildBaseParams(cfg, REFUND_METHOD);
        try {
            params.put("biz_content", objectMapper.writeValueAsString(bizContent));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize biz_content", e);
        }

        String sign = AlipaySignatureUtils.sign(params, cfg.privateKey());
        params.put("sign", sign);

        try {
            String body = buildQueryString(params);
            String response = restTemplate.postForObject(cfg.gatewayUrl(), body, String.class);

            log.debug("Alipay refund response for {}: {}", outTradeNo, response);

            @SuppressWarnings("unchecked")
            Map<String, Object> root = objectMapper.readValue(response, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = (Map<String, Object>)
                    ((Map<String, Object>) root.get("alipay_trade_refund_response"));
            String code = (String) responseMap.get("code");
            if (!"10000".equals(code)) {
                log.warn("Alipay refund failed: code={}, msg={}", code, responseMap.get("msg"));
                return new RefundResult(false, null, outTradeNo, amount);
            }

            String refundFee = (String) responseMap.get("refund_fee");
            BigDecimal refundAmount = refundFee != null ? new BigDecimal(refundFee) : amount;

            log.info("Alipay refund successful: outTradeNo={}, amount={}", outTradeNo, refundAmount);
            return new RefundResult(true, outTradeNo, outTradeNo, refundAmount);
        } catch (Exception e) {
            log.error("Alipay refund error for {}: {}", outTradeNo, e.getMessage(), e);
            return new RefundResult(false, null, outTradeNo, amount);
        }
    }

    @Override
    public String supportedMethod() {
        return "ALIPAY";
    }

    private Map<String, String> buildBaseParams(NeopickProperties.PaymentProperties.AlipayProperties cfg,
                                                 String method) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", cfg.appId());
        params.put("method", method);
        params.put("format", FORMAT);
        params.put("charset", CHARSET);
        params.put("sign_type", AlipaySignatureUtils.signType());
        params.put("timestamp", ALIPAY_TIME.format(LocalDateTime.now()));
        params.put("version", VERSION);
        return params;
    }

    private String buildQueryString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }
}
