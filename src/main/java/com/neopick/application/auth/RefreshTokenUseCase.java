package com.neopick.application.auth;

import com.neopick.domain.auth.TokenPair;
import com.neopick.port.security.TokenProvider;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenUseCase {

    private final TokenProvider tokenProvider;

    public RefreshTokenUseCase(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public TokenPair execute(RefreshTokenCommand command) {
        if (!tokenProvider.validateToken(command.refreshToken())) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
        String userId = tokenProvider.getUserIdFromToken(command.refreshToken());
        String role = tokenProvider.getRoleFromToken(command.refreshToken());
        String newAccessToken = tokenProvider.generateAccessToken(userId, role);
        String newRefreshToken = tokenProvider.generateRefreshToken(userId);
        return new TokenPair(newAccessToken, newRefreshToken);
    }

    public record RefreshTokenCommand(String refreshToken) {}
}
