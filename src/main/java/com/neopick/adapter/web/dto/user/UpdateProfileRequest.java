package com.neopick.adapter.web.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to update user profile fields")
public record UpdateProfileRequest(
        @Schema(description = "New display nickname", example = "RockStar2024", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String nickname,

        @Schema(description = "Gender (MALE or FEMALE)", example = "MALE", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String gender,

        @Schema(description = "New avatar image URL", example = "https://cdn.neopick.com/avatars/new123.jpg", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String avatarUrl
) {}
