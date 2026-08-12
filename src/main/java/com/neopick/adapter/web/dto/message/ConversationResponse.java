package com.neopick.adapter.web.dto.message;

import com.neopick.domain.message.Conversation;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Conversation summary between a student and teacher")
public record ConversationResponse(
        @Schema(description = "Conversation ID (UUID)", example = "990e8400-e29b-41d4-a716-446655440004")
        String id,

        @Schema(description = "Student user ID", example = "user_student_001")
        String studentId,

        @Schema(description = "Teacher ID", example = "42")
        Long teacherId,

        @Schema(description = "Content of the most recent message", example = "See you tomorrow!")
        String lastMessageContent,

        @Schema(description = "Timestamp of the most recent message", example = "2024-06-14T20:30:00")
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
