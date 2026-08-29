package com.neopick.port.security;

import java.time.Instant;

public interface TokenProvider {

    String generateAccessToken(String userId, String role);

    String generateRefreshToken(String userId);

    boolean validateToken(String token);

    String getUserIdFromToken(String token);

    String getRoleFromToken(String token);

    String hashToken(String token);

    Instant getExpirationFromToken(String token);
}
