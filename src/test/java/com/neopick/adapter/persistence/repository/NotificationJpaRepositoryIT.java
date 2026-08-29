package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.NotificationJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("NotificationJpaRepository Integration Tests")
class NotificationJpaRepositoryIT {

    @Autowired private NotificationJpaRepository repository;

    @BeforeEach
    void setUp() {
        repository.save(createNotif("user-1", "BOOKING_CONFIRMED", "Teacher confirmed", true));
        repository.save(createNotif("user-1", "BOOKING_CANCELLED", "Teacher cancelled", false));
        repository.save(createNotif("user-1", "REVIEW_PROMPT", "Leave a review", false));
        repository.save(createNotif("user-2", "BOOKING_CONFIRMED", "Confirmed", true));
    }

    @Nested
    @DisplayName("Find by user")
    class FindByUser {

        @Test
        @DisplayName("should find all notifications for user")
        void shouldFindAllForUser() {
            List<NotificationJpaEntity> results = repository.findByUserIdOrderByCreatedAtDesc(
                    "user-1", PageRequest.of(0, 20));

            assertThat(results).hasSize(3);
            assertThat(results).allMatch(n -> n.getUserId().equals("user-1"));
        }

        @Test
        @DisplayName("should return empty for unknown user")
        void shouldReturnEmptyForUnknown() {
            List<NotificationJpaEntity> results = repository.findByUserIdOrderByCreatedAtDesc(
                    "unknown", PageRequest.of(0, 20));

            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Filter by type")
    class FilterByType {

        @Test
        @DisplayName("should filter by notification type")
        void shouldFilterByType() {
            List<NotificationJpaEntity> results = repository.findByUserIdAndTypeOrderByCreatedAtDesc(
                    "user-1", "BOOKING_CONFIRMED", PageRequest.of(0, 20));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getType()).isEqualTo("BOOKING_CONFIRMED");
        }
    }

    @Nested
    @DisplayName("Unread count")
    class UnreadCount {

        @Test
        @DisplayName("should count unread notifications")
        void shouldCountUnread() {
            long unread = repository.countByUserIdAndReadFalse("user-1");
            assertThat(unread).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Mark all as read")
    class MarkAllAsRead {

        @Test
        @DisplayName("should mark all unread notifications as read")
        void shouldMarkAllAsRead() {
            repository.markAllAsRead("user-1");

            long unread = repository.countByUserIdAndReadFalse("user-1");
            assertThat(unread).isZero();
        }
    }

    private NotificationJpaEntity createNotif(String userId, String type,
                                               String message, boolean read) {
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setType(type);
        entity.setTitle(message);
        entity.setContent(message);
        entity.setRead(read);
        entity.setReferenceId("ref-" + UUID.randomUUID().toString().substring(0, 8));
        return entity;
    }
}
