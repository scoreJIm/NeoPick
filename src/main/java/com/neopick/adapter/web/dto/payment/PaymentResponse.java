package com.neopick.adapter.web.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payment record with status and payment URL")
public record PaymentResponse(
        @Schema(description = "Payment record ID (UUID)", example = "660e8400-e29b-41d4-a716-446655440001")
        String id,

        @Schema(description = "Associated booking ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String bookingId,

        @Schema(description = "Payment amount in CNY", example = "200.00")
        String amount,

        @Schema(description = "Payment method used", example = "WECHAT")
        String method,

        @Schema(description = "Payment status (PENDING, PAID, FAILED, REFUNDED)", example = "PENDING")
        String status,

        @Schema(description = "URL for the user to complete payment", example = "https://pay.neopick.com/wechat/order_abc123")
        String payUrl
) {}
