package com.neopick.adapter.web.security;

import com.neopick.port.security.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final SecurityContextHolder securityContextHolder;

    public JwtAuthenticationFilter(TokenProvider tokenProvider, SecurityContextHolder securityContextHolder) {
        this.tokenProvider = tokenProvider;
        this.securityContextHolder = securityContextHolder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (!token.isBlank() && tokenProvider.validateToken(token)) {
                String userId = tokenProvider.getUserIdFromToken(token);
                String role = tokenProvider.getRoleFromToken(token);
                securityContextHolder.setAuthentication(userId, role);
                MDC.put("userId", userId);
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            securityContextHolder.clear();
        }
    }
}
