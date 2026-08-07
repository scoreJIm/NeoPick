package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.payment.InitiatePaymentRequest;
import com.neopick.adapter.web.dto.payment.PaymentResponse;
import com.neopick.application.payment.InitiatePaymentUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final InitiatePaymentUseCase initiatePaymentUseCase;

    public PaymentController(InitiatePaymentUseCase initiatePaymentUseCase) {
        this.initiatePaymentUseCase = initiatePaymentUseCase;
    }

    @PostMapping
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

    @PostMapping("/callback/wechat")
    public ApiResponse<Void> wechatCallback(@RequestBody String body) {
        return ApiResponse.success();
    }
}
