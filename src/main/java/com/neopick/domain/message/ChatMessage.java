package com.neopick.domain.message;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChatMessage {

    private UUID id;
    private String conversationId;
    private String senderId;
    private String receiverId;
    private String content;
    private MessageType messageType;
    private boolean read;
    private LocalDateTime sentAt;

    private ChatMessage() {
    }

    public ChatMessage(UUID id, String conversationId, String senderId, String receiverId,
                       String content, MessageType messageType) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.messageType = messageType;
        this.read = false;
        this.sentAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getConversationId() { return conversationId; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getContent() { return content; }
    public MessageType getMessageType() { return messageType; }
    public boolean isRead() { return read; }
    public LocalDateTime getSentAt() { return sentAt; }
}
