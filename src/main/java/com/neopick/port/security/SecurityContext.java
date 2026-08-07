package com.neopick.port.security;

import java.util.Optional;

public interface SecurityContext {

    Optional<String> getCurrentUserId();

    Optional<String> getCurrentUserRole();

    String requireCurrentUserId();

    boolean isAuthenticated();
}
