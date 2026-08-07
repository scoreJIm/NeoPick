package com.neopick.adapter.web.security;

import com.neopick.port.security.TokenProvider;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider implements TokenProvider {

    private final SecretKey secretKey;
    private final Duration accessTokenExpiration;
    private final Duration refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${neopick.jwt.secret}") String secret,
            @Value("${neopick.jwt.access-token-expiration}") String accessExpiration,
            @Value("${neopick.jwt.refresh-token-expiration}") String refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = parseDuration(accessExpiration);
        this.refreshTokenExpiration = parseDuration(refreshExpiration);
    }

    @Override
    public String generateAccessToken(String userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpiration)))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public String generateRefreshToken(String userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(refreshTokenExpiration)))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String getUserIdFromToken(String token) {
        return Jwts.parser().verifyWith(secretKey).build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    @Override
    public String getRoleFromToken(String token) {
        return Jwts.parser().verifyWith(secretKey).build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    private Duration parseDuration(String value) {
        if (value.endsWith("d")) {
            return Duration.ofDays(Long.parseLong(value.replace("d", "")));
        }
        if (value.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(value.replace("h", "")));
        }
        if (value.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(value.replace("m", "")));
        }
        return Duration.ofHours(2);
    }
}
