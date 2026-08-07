package com.neopick.adapter.web.dto.message;

import com.neopick.domain.message.ChatMessage;

public record MessageResponse(
        String id,
        String conversationId,
        String senderId,
        String content,
        String messageType,
        boolean read,
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
