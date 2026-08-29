package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.ReviewJpaEntity;
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
@DisplayName("ReviewJpaRepository Integration Tests")
class ReviewJpaRepositoryIT {

    @Autowired private ReviewJpaRepository repository;

    @BeforeEach
    void setUp() {
        repository.save(createReview("student-A", 100L, 5, "Excellent!"));
        repository.save(createReview("student-A", 200L, 4, "Good"));
        repository.save(createReview("student-B", 100L, 3, "OK"));
    }

    @Nested
    @DisplayName("Find by student")
    class FindByStudent {

        @Test
        @DisplayName("should find all reviews by student id")
        void shouldFindByStudentId() {
            List<ReviewJpaEntity> reviews = repository.findByStudentIdOrderByCreatedAtDesc(
                    "student-A", PageRequest.of(0, 20));

            assertThat(reviews).hasSize(2);
            assertThat(reviews).allMatch(r -> r.getStudentId().equals("student-A"));
        }

        @Test
        @DisplayName("should return empty for unknown student")
        void shouldReturnEmptyForUnknown() {
            List<ReviewJpaEntity> reviews = repository.findByStudentIdOrderByCreatedAtDesc(
                    "unknown", PageRequest.of(0, 20));

            assertThat(reviews).isEmpty();
        }
    }

    @Nested
    @DisplayName("Find by teacher")
    class FindByTeacher {

        @Test
        @DisplayName("should find reviews by teacher id")
        void shouldFindByTeacherId() {
            List<ReviewJpaEntity> reviews = repository.findByTeacherIdOrderByCreatedAtDesc(
                    100L, PageRequest.of(0, 20));

            assertThat(reviews).hasSize(2);
            assertThat(reviews).allMatch(r -> r.getTeacherId() == 100L);
        }
    }

    @Nested
    @DisplayName("Check existence by booking")
    class ExistsByBooking {

        @Test
        @DisplayName("should detect duplicate reviews for same booking")
        void shouldDetectDuplicate() {
            assertThat(repository.existsByBookingId("booking-1")).isTrue();
            assertThat(repository.existsByBookingId("nonexistent")).isFalse();
        }
    }

    private ReviewJpaEntity createReview(String studentId, Long teacherId,
                                          int rating, String content) {
        ReviewJpaEntity entity = new ReviewJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setBookingId("booking-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setStudentId(studentId);
        entity.setTeacherId(teacherId);
        entity.setRating(rating);
        entity.setContent(content);
        entity.setTags("professional,patient");
        return entity;
    }
}
