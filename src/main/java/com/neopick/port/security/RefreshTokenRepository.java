package com.neopick.port.security;

import com.neopick.domain.auth.StoredRefreshToken;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    void save(UUID userId, String tokenHash, String familyId, LocalDateTime expiresAt);

    Optional<StoredRefreshToken> findByTokenHash(String tokenHash);

    void markRevoked(String tokenHash, String replacedByTokenHash, LocalDateTime revokedAt);

    void revokeAllByFamilyId(String familyId, LocalDateTime revokedAt);

    void deleteAllByUserId(UUID userId);
}
