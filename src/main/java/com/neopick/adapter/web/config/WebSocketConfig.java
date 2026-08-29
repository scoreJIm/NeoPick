package com.neopick.adapter.web.config;

import com.neopick.adapter.web.websocket.WebSocketAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * STOMP-over-WebSocket configuration for real-time chat.
 * Configures message broker, STOMP endpoints with SockJS fallback,
 * and JWT authentication via channel interceptor.
 */
@Configuration
@EnableWebSocketMessageBroker
@Profile("!test")
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final int MAX_MESSAGE_SIZE = 64 * 1024; // 64 KB
    private static final int SEND_BUFFER_SIZE_LIMIT = 512 * 1024; // 512 KB

    private final WebSocketAuthInterceptor authInterceptor;
    private final TaskScheduler taskScheduler;

    public WebSocketConfig(WebSocketAuthInterceptor authInterceptor, TaskScheduler taskScheduler) {
        this.authInterceptor = authInterceptor;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker for /topic (public) and /queue (user-specific)
        // Heartbeat: 10000ms server-to-client, 10000ms client-to-server
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(taskScheduler);
        // Application destination prefix: messages routed to @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        // User destination prefix: messages sent to /user/... are routed to specific users
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureWebSocketTransport(@NonNull WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(MAX_MESSAGE_SIZE);
        registration.setSendBufferSizeLimit(SEND_BUFFER_SIZE_LIMIT);
        registration.setSendTimeLimit(20 * 1000);
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
        registration.taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(8);
    }

    @Override
    public void configureClientOutboundChannel(@NonNull ChannelRegistration registration) {
        registration.taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(8);
    }
}
