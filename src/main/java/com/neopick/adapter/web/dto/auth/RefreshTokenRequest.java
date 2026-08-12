package com.neopick.adapter.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to refresh an expired access token")
public record RefreshTokenRequest(
        @NotBlank
        @Schema(description = "Valid refresh token obtained during login or previous refresh", example = "eyJhbGciOiJIUzI1NiIs...", requiredMode = Schema.RequiredMode.REQUIRED)
        String refreshToken
) {}
