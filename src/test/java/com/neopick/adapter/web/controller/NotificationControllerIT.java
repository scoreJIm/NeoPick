package com.neopick.adapter.web.controller;

import com.neopick.application.notification.NotificationUseCase;
import com.neopick.domain.notification.*;
import com.neopick.port.security.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(NotificationUseCase.class)
@DisplayName("Notification API Integration Tests")
class NotificationControllerIT {

    @Autowired private MockMvc mockMvc;

    @MockBean private NotificationRepository notificationRepository;
    @MockBean private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        when(securityContext.requireCurrentUserId()).thenReturn("student-001");
        when(securityContext.getCurrentUserId()).thenReturn(Optional.of("student-001"));
    }

    @Nested
    @DisplayName("GET /api/v1/notifications — List notifications")
    class ListNotifications {

        @Test
        @DisplayName("should return notifications")
        void shouldReturnNotifications() throws Exception {
            var notif = new Notification(NotificationId.generate(), "student-001",
                    "Teacher John confirmed your booking", NotificationType.BOOKING_CONFIRMED,
                    "booking-123");
            when(notificationRepository.findByUserId(eq("student-001"), isNull(), anyInt(), anyInt()))
                    .thenReturn(List.of(notif));

            mockMvc.perform(get("/api/v1/notifications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].type").value("BOOKING_CONFIRMED"));
        }

        @Test
        @DisplayName("should filter by notification type")
        void shouldFilterByType() throws Exception {
            when(notificationRepository.findByUserId(eq("student-001"), eq(NotificationType.BOOKING_CONFIRMED), anyInt(), anyInt()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/v1/notifications")
                            .param("type", "BOOKING_CONFIRMED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/notifications/{id}/read — Mark as read")
    class MarkAsRead {

        @Test
        @DisplayName("should mark notification as read")
        void shouldMarkAsRead() throws Exception {
            mockMvc.perform(put("/api/v1/notifications/{id}/read", "notif-1"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/notifications/read-all — Mark all as read")
    class MarkAllAsRead {

        @Test
        @DisplayName("should mark all notifications as read")
        void shouldMarkAllAsRead() throws Exception {
            mockMvc.perform(put("/api/v1/notifications/read-all"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/unread-count — Unread count")
    class UnreadCount {

        @Test
        @DisplayName("should return unread count")
        void shouldReturnUnreadCount() throws Exception {
            when(notificationRepository.countUnread("student-001")).thenReturn(5L);

            mockMvc.perform(get("/api/v1/notifications/unread-count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.count").value(5));
        }
    }
}
