package com.neopick.application.auth;

import com.neopick.domain.auth.StoredRefreshToken;
import com.neopick.domain.auth.TokenPair;
import com.neopick.infrastructure.security.SecurityEventLogger;
import com.neopick.infrastructure.security.SecurityEventLogger.EventType;
import com.neopick.port.security.RefreshTokenRepository;
import com.neopick.port.security.TokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenUseCase {

    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityEventLogger securityEventLogger;

    public RefreshTokenUseCase(TokenProvider tokenProvider,
                               RefreshTokenRepository refreshTokenRepository,
                               SecurityEventLogger securityEventLogger) {
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.securityEventLogger = securityEventLogger;
    }

    @Transactional
    public TokenPair execute(RefreshTokenCommand command) {
        if (!tokenProvider.validateToken(command.refreshToken())) {
            throw new TokenExpiredException("Refresh token is invalid or expired");
        }

        String tokenHash = tokenProvider.hashToken(command.refreshToken());
        Optional<StoredRefreshToken> storedTokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        if (storedTokenOpt.isEmpty()) {
            throw new TokenExpiredException("Refresh token not found");
        }

        StoredRefreshToken storedToken = storedTokenOpt.get();

        if (LocalDateTime.now().isAfter(storedToken.expiresAt())) {
            throw new TokenExpiredException("Refresh token has expired");
        }

        if (storedToken.revoked()) {
            if (storedToken.familyId() != null) {
                refreshTokenRepository.revokeAllByFamilyId(storedToken.familyId(), LocalDateTime.now());
                securityEventLogger.logEvent(EventType.TOKEN_REUSE_DETECTED,
                        storedToken.userId().toString(), null, null);
                throw new TokenReuseDetectedException(
                        "Token reuse detected. All sessions invalidated.");
            }
            throw new TokenRevokedException("Refresh token has been revoked");
        }

        String userId = tokenProvider.getUserIdFromToken(command.refreshToken());
        String role = tokenProvider.getRoleFromToken(command.refreshToken());

        String newAccessToken = tokenProvider.generateAccessToken(userId, role);
        String newRefreshToken = tokenProvider.generateRefreshToken(userId);

        String newTokenHash = tokenProvider.hashToken(newRefreshToken);
        String familyId = storedToken.familyId() != null
                ? storedToken.familyId()
                : UUID.randomUUID().toString();
        LocalDateTime newExpiresAt = LocalDateTime.ofInstant(
                tokenProvider.getExpirationFromToken(newRefreshToken), ZoneId.systemDefault());

        refreshTokenRepository.markRevoked(tokenHash, newTokenHash, LocalDateTime.now());
        refreshTokenRepository.save(UUID.fromString(userId), newTokenHash, familyId, newExpiresAt);

        securityEventLogger.logEvent(EventType.TOKEN_REFRESHED, userId, null, null);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(LogoutCommand command) {
        String tokenHash = tokenProvider.hashToken(command.refreshToken());
        refreshTokenRepository.markRevoked(tokenHash, null, LocalDateTime.now());
        securityEventLogger.logEvent(EventType.LOGOUT, null, null, null);
    }

    public record RefreshTokenCommand(String refreshToken) {}

    public record LogoutCommand(String refreshToken) {}

    public static class TokenExpiredException extends RuntimeException {
        public TokenExpiredException(String message) {
            super(message);
        }
    }

    public static class TokenRevokedException extends RuntimeException {
        public TokenRevokedException(String message) {
            super(message);
        }
    }

    public static class TokenReuseDetectedException extends RuntimeException {
        public TokenReuseDetectedException(String message) {
            super(message);
        }
    }
}
