package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.notification.NotificationResponse;
import com.neopick.application.notification.NotificationUseCase;
import com.neopick.infrastructure.ratelimit.RateLimit;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "User notification management: list, read, and unread count")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationUseCase notificationUseCase;

    public NotificationController(NotificationUseCase notificationUseCase) {
        this.notificationUseCase = notificationUseCase;
    }

    @GetMapping
    @RateLimit(limit = 60, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.notifications.list", description = "List notifications")
    @Operation(summary = "List notifications", description = "Returns the authenticated user's notifications, optionally filtered by type. Results are paginated.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notifications returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ApiResponse<List<NotificationResponse>> list(
            @Parameter(description = "Filter by notification type (BOOKING, PAYMENT, REVIEW, SYSTEM)") @RequestParam(required = false) String type,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        var notifications = notificationUseCase.list(type, page, size);
        return ApiResponse.success(notifications.stream().map(NotificationResponse::from).toList());
    }

    @PutMapping("/{id}/read")
    @RateLimit(limit = 60, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.notifications.mark_read", description = "Mark notification as read")
    @Operation(summary = "Mark notification as read", description = "Marks a single notification as read by its ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notification marked as read"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notification not found", content = @Content)
    })
    public ApiResponse<Void> markAsRead(
            @Parameter(description = "Notification ID (UUID format)") @PathVariable String id) {
        notificationUseCase.markAsRead(id);
        return ApiResponse.success();
    }

    @PutMapping("/read-all")
    @RateLimit(limit = 60, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.notifications.mark_all_read", description = "Mark all notifications as read")
    @Operation(summary = "Mark all notifications as read", description = "Marks all of the authenticated user's notifications as read in a single operation.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All notifications marked as read"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ApiResponse<Void> markAllAsRead() {
        notificationUseCase.markAllAsRead();
        return ApiResponse.success();
    }

    @GetMapping("/unread-count")
    @RateLimit(limit = 60, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.notifications.unread_count", description = "Get unread notification count")
    @Operation(summary = "Get unread notification count", description = "Returns the total count of unread notifications for the authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Unread count returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.success(Map.of("count", notificationUseCase.unreadCount()));
    }
}
