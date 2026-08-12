package com.neopick.adapter.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to logout and revoke the current refresh token")
public record LogoutRequest(
        @NotBlank
        @Schema(description = "Refresh token to revoke", example = "eyJhbGciOiJIUzI1NiIs...", requiredMode = Schema.RequiredMode.REQUIRED)
        String refreshToken
) {}
