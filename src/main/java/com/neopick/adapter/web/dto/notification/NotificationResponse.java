package com.neopick.adapter.web.dto.notification;

import com.neopick.domain.notification.Notification;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Notification details for the authenticated user")
public record NotificationResponse(
        @Schema(description = "Notification ID (UUID)", example = "880e8400-e29b-41d4-a716-446655440003")
        String id,

        @Schema(description = "Notification title", example = "Booking Confirmed")
        String title,

        @Schema(description = "Notification content/body", example = "Your booking with Zhang Wei has been confirmed for June 15 at 14:00.")
        String content,

        @Schema(description = "Notification type (BOOKING, PAYMENT, REVIEW, SYSTEM)", example = "BOOKING")
        String type,

        @Schema(description = "ID of the related entity (booking, payment, etc.)", example = "550e8400-e29b-41d4-a716-446655440000")
        String referenceId,

        @Schema(description = "Whether the notification has been read", example = "false")
        boolean read,

        @Schema(description = "Notification creation timestamp", example = "2024-06-14T09:05:00")
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
