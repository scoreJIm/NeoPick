package com.neopick.adapter.web.dto.message;

import com.neopick.domain.message.Conversation;

public record ConversationResponse(
        String id,
        String studentId,
        Long teacherId,
        String lastMessageContent,
        String lastMessageAt
) {
    public static ConversationResponse from(Conversation c) {
        return new ConversationResponse(
                c.getId().value().toString(),
                c.getStudentId(),
                c.getTeacherId(),
                c.getLastMessageContent(),
                c.getLastMessageAt() != null ? c.getLastMessageAt().toString() : null
        );
    }
}
