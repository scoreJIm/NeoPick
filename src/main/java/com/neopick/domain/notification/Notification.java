package com.neopick.domain.notification;

import java.time.LocalDateTime;

public class Notification {

    private NotificationId id;
    private String userId;
    private String title;
    private String content;
    private NotificationType type;
    private String referenceId;
    private boolean read;
    private LocalDateTime createdAt;

    private Notification() {
    }

    public Notification(NotificationId id, String userId, String title, String content,
                        NotificationType type, String referenceId) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.referenceId = referenceId;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.read = true;
    }

    public NotificationId getId() { return id; }
    public String getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public NotificationType getType() { return type; }
    public String getReferenceId() { return referenceId; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
