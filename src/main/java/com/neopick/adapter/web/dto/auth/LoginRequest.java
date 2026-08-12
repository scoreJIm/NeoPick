package com.neopick.adapter.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login request with phone number and SMS verification code")
public record LoginRequest(
        @NotBlank
        @Schema(description = "Phone number in international format", example = "+8613800138000", requiredMode = Schema.RequiredMode.REQUIRED)
        String phone,

        @NotBlank
        @Schema(description = "6-digit SMS verification code", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
        String code
) {}
