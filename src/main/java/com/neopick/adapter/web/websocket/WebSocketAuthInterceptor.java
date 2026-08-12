package com.neopick.adapter.web.websocket;

import com.neopick.adapter.web.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * Intercepts STOMP CONNECT frames to validate JWT tokens before establishing
 * a WebSocket session. Extracts user identity from the token and stores it
 * in session attributes for downstream message handling.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public WebSocketAuthInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = extractToken(accessor);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            log.warn("STOMP CONNECT rejected: invalid or missing JWT token from session {}",
                    accessor.getSessionId());
            throw new MessagingException("Authentication failed: invalid or missing JWT token");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(token);
        accessor.setUser(new StompPrincipal(userId));
        accessor.getSessionAttributes().put("userId", userId);
        log.info("STOMP CONNECT authenticated: userId={}, sessionId={}", userId, accessor.getSessionId());
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Try the Authorization native header first
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null) {
            // Fallback: try the standard login/passcode STOMP headers for token
            authHeader = accessor.getFirstNativeHeader("authorization");
        }
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        // Fallback: check if token was passed as the STOMP login header
        String loginToken = accessor.getLogin();
        if (loginToken != null && !loginToken.isBlank()) {
            return loginToken;
        }
        return null;
    }
}
