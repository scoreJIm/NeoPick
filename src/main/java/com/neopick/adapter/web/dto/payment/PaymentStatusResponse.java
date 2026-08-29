package com.neopick.adapter.web.dto.payment;

public record PaymentStatusResponse(
        String outTradeNo,
        String tradeNo,
        String totalAmount,
        String tradeStatus,
        boolean success
) {}
