package com.neopick.adapter.web.dto.message;

import com.neopick.domain.message.ChatMessage;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Individual chat message within a conversation")
public record MessageResponse(
        @Schema(description = "Message ID", example = "aa0e8400-e29b-41d4-a716-446655440005")
        String id,

        @Schema(description = "Conversation ID this message belongs to", example = "990e8400-e29b-41d4-a716-446655440004")
        String conversationId,

        @Schema(description = "ID of the message sender", example = "user_student_001")
        String senderId,

        @Schema(description = "Message content text", example = "Hi, are you available this Saturday?")
        String content,

        @Schema(description = "Message type (TEXT, IMAGE, SYSTEM)", example = "TEXT")
        String messageType,

        @Schema(description = "Whether the message has been read by the recipient", example = "true")
        boolean read,

        @Schema(description = "Message sent timestamp", example = "2024-06-14T19:45:00")
        String sentAt
) {
    public static MessageResponse from(ChatMessage m) {
        return new MessageResponse(
                m.getId().toString(),
                m.getConversationId(),
                m.getSenderId(),
                m.getContent(),
                m.getMessageType().name(),
                m.isRead(),
                m.getSentAt() != null ? m.getSentAt().toString() : null
        );
    }
}
