package com.neopick.adapter.web.dto.user;

public record UserResponse(
        String id,
        String phone,
        String nickname,
        String avatarUrl,
        String gender,
        String role,
        String status,
        String registeredAt,
        String lastLoginAt
) {}
