package com.neopick.adapter.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login response containing JWT tokens and user profile")
public record LoginResponse(
        @Schema(description = "JWT access token for API authentication", example = "eyJhbGciOiJIUzI1NiIs...")
        String accessToken,

        @Schema(description = "JWT refresh token for obtaining new access tokens", example = "eyJhbGciOiJIUzI1NiIs...")
        String refreshToken,

        @Schema(description = "Authenticated user's profile information")
        UserProfile user
) {

    @Schema(description = "User profile summary returned after login")
    public record UserProfile(
            @Schema(description = "User ID", example = "550e8400-e29b-41d4-a716-446655440000")
            String id,

            @Schema(description = "Masked phone number", example = "+86138****8000")
            String phone,

            @Schema(description = "User nickname", example = "GuitarFan99")
            String nickname,

            @Schema(description = "Avatar image URL", example = "https://cdn.neopick.com/avatars/user123.jpg")
            String avatarUrl,

            @Schema(description = "User role", example = "STUDENT")
            String role
    ) {}
}
