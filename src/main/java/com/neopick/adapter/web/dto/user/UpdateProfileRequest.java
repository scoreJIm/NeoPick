package com.neopick.adapter.web.dto.user;

public record UpdateProfileRequest(
        String nickname,
        String gender,
        String avatarUrl
) {}
