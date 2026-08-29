package com.neopick.adapter.web.websocket;

import com.neopick.adapter.web.dto.message.MessageResponse;
import com.neopick.application.message.MessageUseCase;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * STOMP message controller for real-time chat.
 * Handles chat messages, typing indicators, and read receipts via WebSocket.
 * Uses {@code @MessageMapping} to route STOMP SEND frames to handler methods.
 */
@Controller
@ConditionalOnBean(SimpMessagingTemplate.class)
public class ChatMessageController {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageController.class);
    private static final int MAX_MESSAGE_LENGTH = 5000;

    private final MessageUseCase messageUseCase;
    private final SimpMessagingTemplate messagingTemplate;
    private final Counter messagesSentCounter;

    public ChatMessageController(MessageUseCase messageUseCase,
                                 SimpMessagingTemplate messagingTemplate,
                                 MeterRegistry meterRegistry) {
        this.messageUseCase = messageUseCase;
        this.messagingTemplate = messagingTemplate;
        this.messagesSentCounter = Counter.builder("neopick.websocket.messages.sent")
                .description("Total number of chat messages sent via WebSocket")
                .register(meterRegistry);
    }

    /**
     * Receive and deliver a chat message for a conversation.
     * The message is persisted, then delivered to both sender (confirmation)
     * and recipient via user queues.
     */
    @MessageMapping("/chat/{conversationId}")
    @SendToUser("/queue/chat/{conversationId}")
    public MessageResponse handleMessage(@DestinationVariable String conversationId,
                                          @Payload Map<String, String> payload,
                                          Principal principal,
                                          SimpMessageHeaderAccessor accessor) {
        String senderId = getUserId(principal, accessor);
        String content = payload.get("content");

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content must not be empty");
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message content exceeds maximum length of "
                    + MAX_MESSAGE_LENGTH);
        }

        log.debug("Chat message from userId={} to conversationId={}", senderId, conversationId);

        var command = new MessageUseCase.SendMessageCommand(conversationId, content);
        var savedMessage = messageUseCase.sendMessage(senderId, command);
        messagesSentCounter.increment();

        MessageResponse response = MessageResponse.from(savedMessage);

        // Deliver to the recipient via their user-specific queue
        String recipientId = savedMessage.getReceiverId();
        messagingTemplate.convertAndSendToUser(recipientId,
                "/queue/chat/" + conversationId, response);

        return response;
    }

    /**
     * Broadcast a typing indicator to the other participant in the conversation.
     */
    @MessageMapping("/chat/{conversationId}/typing")
    public void handleTyping(@DestinationVariable String conversationId,
                              @Payload Map<String, String> payload,
                              Principal principal,
                              SimpMessageHeaderAccessor accessor) {
        String senderId = getUserId(principal, accessor);
        String recipientId = payload.get("recipientId");
        boolean isTyping = Boolean.parseBoolean(payload.getOrDefault("typing", "false"));

        if (recipientId == null || recipientId.isBlank()) {
            log.warn("Typing indicator missing recipientId from userId={}", senderId);
            return;
        }

        log.debug("Typing indicator: userId={}, conversationId={}, typing={}",
                senderId, conversationId, isTyping);

        Map<String, Object> typingEvent = Map.of(
                "conversationId", conversationId,
                "senderId", senderId,
                "typing", isTyping
        );
        messagingTemplate.convertAndSendToUser(recipientId,
                "/queue/chat/" + conversationId + "/typing", typingEvent);
    }

    /**
     * Mark messages as read and notify the original sender.
     */
    @MessageMapping("/chat/{conversationId}/read")
    public void handleReadReceipt(@DestinationVariable String conversationId,
                                   Principal principal,
                                   SimpMessageHeaderAccessor accessor) {
        String readerId = getUserId(principal, accessor);

        log.debug("Read receipt: userId={}, conversationId={}", readerId, conversationId);

        messageUseCase.markMessagesAsRead(conversationId, readerId);

        Map<String, Object> readEvent = Map.of(
                "conversationId", conversationId,
                "readerId", readerId,
                "readAt", java.time.Instant.now().toString()
        );

        // Notify both participants about the read receipt
        messagingTemplate.convertAndSend("/topic/chat/" + conversationId + "/read", readEvent);
    }

    private String getUserId(Principal principal, SimpMessageHeaderAccessor accessor) {
        if (principal != null && principal.getName() != null) {
            return principal.getName();
        }
        Object userIdFromSession = accessor.getSessionAttributes().get("userId");
        if (userIdFromSession != null) {
            return userIdFromSession.toString();
        }
        throw new IllegalStateException("User not authenticated in WebSocket session");
    }
}
