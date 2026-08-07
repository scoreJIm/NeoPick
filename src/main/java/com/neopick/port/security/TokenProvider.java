package com.neopick.port.security;

public interface TokenProvider {

    String generateAccessToken(String userId, String role);

    String generateRefreshToken(String userId);

    boolean validateToken(String token);

    String getUserIdFromToken(String token);

    String getRoleFromToken(String token);
}
