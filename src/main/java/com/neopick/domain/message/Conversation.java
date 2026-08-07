package com.neopick.domain.message;

import com.neopick.domain.common.AggregateRoot;

import java.time.LocalDateTime;

public class Conversation implements AggregateRoot {

    private ConversationId id;
    private String studentId;
    private Long teacherId;
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;

    private Conversation() {
    }

    public Conversation(ConversationId id, String studentId, Long teacherId) {
        this.id = id;
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.createdAt = LocalDateTime.now();
    }

    public void updateLastMessage(String content, LocalDateTime at) {
        this.lastMessageContent = content;
        this.lastMessageAt = at;
    }

    public ConversationId getId() { return id; }
    public String getStudentId() { return studentId; }
    public Long getTeacherId() { return teacherId; }
    public String getLastMessageContent() { return lastMessageContent; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
