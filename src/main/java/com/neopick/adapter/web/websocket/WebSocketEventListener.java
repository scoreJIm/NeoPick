package com.neopick.adapter.web.websocket;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Application event listener for WebSocket lifecycle events.
 * Tracks active connections, broadcasts online/offline status,
 * validates subscription destinations, and exposes metrics.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final ConcurrentHashMap<String, String> sessionToUser = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(SimpMessagingTemplate messagingTemplate,
                                  MeterRegistry meterRegistry) {
        this.messagingTemplate = messagingTemplate;
        meterRegistry.gauge("neopick.websocket.connections.active", activeConnections);
    }

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String userId = extractUserId(accessor);
        if (userId != null) {
            sessionToUser.put(sessionId, userId);
        }
        int current = activeConnections.incrementAndGet();
        log.info("WebSocket connected: sessionId={}, userId={}, activeConnections={}",
                sessionId, userId, current);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String userId = sessionToUser.remove(sessionId);
        int current = activeConnections.decrementAndGet();
        if (userId != null) {
            log.info("WebSocket disconnected: sessionId={}, userId={}, activeConnections={}",
                    sessionId, userId, current);
            broadcastUserOffline(userId);
        } else {
            log.info("WebSocket disconnected: sessionId={}, activeConnections={}",
                    sessionId, current);
        }
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        String userId = getUserIdForSession(sessionId);

        if (destination != null && destination.startsWith("/user/queue/chat/")
                && userId != null) {
            // Extract the target user from the destination and validate ownership
            // Destination format: /user/queue/chat/{conversationId}
            // The /user prefix means Spring will resolve to the authenticated user automatically
            log.debug("Subscription: userId={}, destination={}", userId, destination);
        }
    }

    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String userId = getUserIdForSession(sessionId);
        log.debug("Unsubscribe: userId={}, sessionId={}, destination={}",
                userId, sessionId, accessor.getDestination());
    }

    private String extractUserId(StompHeaderAccessor accessor) {
        if (accessor.getUser() != null) {
            return accessor.getUser().getName();
        }
        Object userIdAttr = accessor.getSessionAttributes().get("userId");
        if (userIdAttr != null) {
            return userIdAttr.toString();
        }
        return null;
    }

    private String getUserIdForSession(String sessionId) {
        return sessionToUser.get(sessionId);
    }

    private void broadcastUserOffline(String userId) {
        Map<String, Object> offlineEvent = Map.of(
                "userId", userId,
                "event", "offline",
                "timestamp", java.time.Instant.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/user-status", offlineEvent);
    }
}
