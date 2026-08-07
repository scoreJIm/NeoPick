package com.neopick.adapter.web.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record InitiatePaymentRequest(
        @NotBlank String bookingId,
        @NotBlank String method
) {}
