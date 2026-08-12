package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.payment.InitiatePaymentRequest;
import com.neopick.adapter.web.dto.payment.PaymentResponse;
import com.neopick.adapter.web.dto.payment.PaymentStatusResponse;
import com.neopick.application.payment.HandlePaymentCallbackUseCase;
import com.neopick.application.payment.InitiatePaymentUseCase;
import com.neopick.port.payment.PaymentGateway;
import com.neopick.infrastructure.ratelimit.RateLimit;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Payment initiation and callback handling")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final InitiatePaymentUseCase initiatePaymentUseCase;
    private final HandlePaymentCallbackUseCase handlePaymentCallbackUseCase;
    private final PaymentGateway paymentGateway;

    public PaymentController(InitiatePaymentUseCase initiatePaymentUseCase,
                              HandlePaymentCallbackUseCase handlePaymentCallbackUseCase,
                              PaymentGateway paymentGateway) {
        this.initiatePaymentUseCase = initiatePaymentUseCase;
        this.handlePaymentCallbackUseCase = handlePaymentCallbackUseCase;
        this.paymentGateway = paymentGateway;
    }

    @PostMapping
    @RateLimit(limit = 5, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.payments.initiate", description = "Initiate payment")
    @Operation(summary = "Initiate payment for a booking", description = "Initiates a payment for a confirmed booking. Returns a payment record with a payment URL for the selected method (Alipay or WeChat).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment initiated successfully - pay URL returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payment method or missing booking ID", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found", content = @Content)
    })
    public ApiResponse<PaymentResponse> initiate(@Valid @RequestBody InitiatePaymentRequest request) {
        var result = initiatePaymentUseCase.execute(
                new InitiatePaymentUseCase.InitiatePaymentCommand(request.bookingId(), request.method()));
        return ApiResponse.success(new PaymentResponse(
                result.payment().getId().value().toString(),
                result.payment().getBookingId(),
                result.payment().getAmount().toString(),
                result.payment().getMethod().name(),
                result.payment().getStatus().name(),
                result.payUrl()));
    }

    @PostMapping("/callback/alipay")
    @RateLimit(limit = 60, windowSeconds = 60, scope = "IP")
    @Timed(value = "neopick.payments.alipay_callback", description = "Alipay payment callback")
    @Operation(summary = "Alipay payment callback", description = "Handles asynchronous payment notification from Alipay. Verifies signature, updates payment and booking status, and creates notifications.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Callback processed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Callback verification failed", content = @Content)
    })
    public ApiResponse<String> alipayCallback(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        var result = handlePaymentCallbackUseCase.execute(params, "ALIPAY");
        if (result.success()) {
            return ApiResponse.success("success");
        }
        return ApiResponse.error(400, "Callback processing failed");
    }

    @PostMapping("/callback/wechat")
    @RateLimit(limit = 60, windowSeconds = 60, scope = "IP")
    @Timed(value = "neopick.payments.wechat_callback", description = "WeChat payment callback")
    @Operation(summary = "WeChat payment callback", description = "Handles asynchronous payment notification from WeChat Pay. Verifies the notification signature, decrypts the resource, and updates payment and booking status.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Callback processed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid callback payload", content = @Content)
    })
    public Map<String, String> wechatCallback(@RequestBody String body, @RequestHeader HttpHeaders headers) {
        Map<String, String> params = new HashMap<>();
        params.put("body", body);
        params.put("wechatpay-signature", headers.getFirst("Wechatpay-Signature"));
        params.put("wechatpay-timestamp", headers.getFirst("Wechatpay-Timestamp"));
        params.put("wechatpay-nonce", headers.getFirst("Wechatpay-Nonce"));
        params.put("wechatpay-serial", headers.getFirst("Wechatpay-Serial"));

        Map<String, String> response = new HashMap<>();
        try {
            var result = handlePaymentCallbackUseCase.execute(params, "WECHAT");
            if (result.success()) {
                response.put("code", "SUCCESS");
                response.put("message", "成功");
            } else {
                response.put("code", "FAIL");
                response.put("message", "失败");
            }
        } catch (Exception e) {
            log.warn("WeChat callback processing failed", e);
            response.put("code", "FAIL");
            response.put("message", "失败");
        }
        return response;
    }

    @GetMapping("/{paymentId}/status")
    @Timed(value = "neopick.payments.status", description = "Query payment status")
    @Operation(summary = "Query payment status from gateway", description = "Queries the current payment status from the payment provider (Alipay).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Payment not found", content = @Content)
    })
    public ApiResponse<PaymentStatusResponse> queryStatus(@PathVariable String paymentId) {
        PaymentGateway.QueryResult result = paymentGateway.queryPayment(paymentId);
        return ApiResponse.success(new PaymentStatusResponse(
                result.outTradeNo(),
                result.tradeNo(),
                result.totalAmount() != null ? result.totalAmount().toString() : null,
                result.tradeStatus(),
                result.success()));
    }

    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }
}
