package com.neopick.adapter.web.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User profile information")
public record UserResponse(
        @Schema(description = "User ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "Masked phone number for privacy", example = "+86138****8000")
        String phone,

        @Schema(description = "Display nickname", example = "GuitarFan99")
        String nickname,

        @Schema(description = "Avatar image URL", example = "https://cdn.neopick.com/avatars/abc123.jpg")
        String avatarUrl,

        @Schema(description = "Gender (MALE, FEMALE)", example = "MALE")
        String gender,

        @Schema(description = "User role (STUDENT, TEACHER, ADMIN)", example = "STUDENT")
        String role,

        @Schema(description = "Account status (ACTIVE, INACTIVE, BANNED)", example = "ACTIVE")
        String status,

        @Schema(description = "Registration timestamp (ISO 8601)", example = "2024-01-15T10:30:00")
        String registeredAt,

        @Schema(description = "Last login timestamp (ISO 8601)", example = "2024-06-01T08:15:00")
        String lastLoginAt
) {}
