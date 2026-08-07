package com.neopick.adapter.web.dto.notification;

import com.neopick.domain.notification.Notification;

public record NotificationResponse(
        String id,
        String title,
        String content,
        String type,
        String referenceId,
        boolean read,
        String createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId().value().toString(),
                n.getTitle(),
                n.getContent(),
                n.getType().name(),
                n.getReferenceId(),
                n.isRead(),
                n.getCreatedAt() != null ? n.getCreatedAt().toString() : null
        );
    }
}
