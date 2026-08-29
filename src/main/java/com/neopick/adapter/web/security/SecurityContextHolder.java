package com.neopick.adapter.web.security;

import com.neopick.port.security.SecurityContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Optional;

@Component
@RequestScope
public class SecurityContextHolder implements SecurityContext {

    private String userId;
    private String userRole;
    private boolean authenticated;

    public void setAuthentication(String userId, String role) {
        this.userId = userId;
        this.userRole = role;
        this.authenticated = true;
    }

    public void clear() {
        this.userId = null;
        this.userRole = null;
        this.authenticated = false;
    }

    @Override
    public Optional<String> getCurrentUserId() {
        return Optional.ofNullable(userId);
    }

    @Override
    public Optional<String> getCurrentUserRole() {
        return Optional.ofNullable(userRole);
    }

    @Override
    public String requireCurrentUserId() {
        return getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("User not authenticated"));
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }
}
