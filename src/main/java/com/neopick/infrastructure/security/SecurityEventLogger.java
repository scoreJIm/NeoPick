package com.neopick.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SecurityEventLogger {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventLogger.class);

    public enum EventType {
        TOKEN_ISSUED,
        TOKEN_REFRESHED,
        TOKEN_REUSE_DETECTED,
        TOKEN_REVOKED,
        TOKEN_FAMILY_REVOKED,
        LOGOUT
    }

    public void logEvent(EventType eventType, String userId, String ipAddress,
                          String userAgent) {
        String message = String.format(
                "SecurityEvent: type=%s, userId=%s, ip=%s, userAgent=%s, timestamp=%s",
                eventType, userId, ipAddress != null ? ipAddress : "unknown",
                userAgent != null ? userAgent : "unknown", Instant.now());

        if (eventType == EventType.TOKEN_REUSE_DETECTED
                || eventType == EventType.TOKEN_FAMILY_REVOKED) {
            log.warn(message);
        } else {
            log.info(message);
        }
    }
}
