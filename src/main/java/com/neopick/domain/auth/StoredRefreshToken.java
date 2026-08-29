package com.neopick.domain.auth;

import java.time.LocalDateTime;
import java.util.UUID;

public record StoredRefreshToken(
        UUID id,
        UUID userId,
        String tokenHash,
        String familyId,
        boolean revoked,
        String replacedByToken,
        LocalDateTime expiresAt,
        LocalDateTime revokedAt,
        LocalDateTime createdAt
) {}
