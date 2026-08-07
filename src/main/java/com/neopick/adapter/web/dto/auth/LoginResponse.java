package com.neopick.adapter.web.dto.auth;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UserProfile user
) {

    public record UserProfile(
            String id,
            String phone,
            String nickname,
            String avatarUrl,
            String role
    ) {}
}
