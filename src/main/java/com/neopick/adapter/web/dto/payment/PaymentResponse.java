package com.neopick.adapter.web.dto.payment;

public record PaymentResponse(
        String id,
        String bookingId,
        String amount,
        String method,
        String status,
        String payUrl
) {}
