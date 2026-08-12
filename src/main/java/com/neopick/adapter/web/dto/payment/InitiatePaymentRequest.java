package com.neopick.adapter.web.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to initiate payment for a booking")
public record InitiatePaymentRequest(
        @NotBlank
        @Schema(description = "ID of the booking to pay for", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
        String bookingId,

        @NotBlank
        @Schema(description = "Payment method (e.g., WECHAT, ALIPAY)", example = "WECHAT", requiredMode = Schema.RequiredMode.REQUIRED)
        String method
) {}
