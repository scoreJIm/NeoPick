package com.neopick.adapter.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to send SMS verification code")
public record SendSmsRequest(
        @NotBlank
        @Pattern(regexp = "^\\+86\\d{11}$", message = "Invalid phone number format")
        @Schema(description = "Phone number in international format with country code", example = "+8613800138000", requiredMode = Schema.RequiredMode.REQUIRED)
        String phone
) {}
