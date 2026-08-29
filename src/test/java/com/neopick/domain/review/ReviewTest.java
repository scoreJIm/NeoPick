package com.neopick.domain.review;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Review")
class ReviewTest {

    @Nested
    @DisplayName("Rating validation")
    class RatingValidation {

        @Test
        @DisplayName("should create review with valid rating 1-5")
        void shouldCreateWithValidRating() {
            for (int r = 1; r <= 5; r++) {
                int rating = r;
                Review review = new Review(ReviewId.generate(), "booking-1",
                        "student-1", 100L, rating, "Good", List.of());
                assertThat(review.getRating()).isEqualTo(rating);
            }
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 6, -1, 10})
        @DisplayName("should reject rating outside 1-5 range")
        void shouldRejectInvalidRating(int invalidRating) {
            assertThatThrownBy(() -> new Review(ReviewId.generate(), "booking-1",
                    "student-1", 100L, invalidRating, "Bad", List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rating must be between 1 and 5");
        }
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("should set id, booking, student, teacher, content, tags")
        void shouldSetAllFields() {
            Review review = new Review(ReviewId.generate(), "booking-abc",
                    "student-1", 100L, 5, "Amazing teacher!",
                    List.of("professional", "patient"));

            assertThat(review.getId()).isNotNull();
            assertThat(review.getBookingId()).isEqualTo("booking-abc");
            assertThat(review.getStudentId()).isEqualTo("student-1");
            assertThat(review.getTeacherId()).isEqualTo(100L);
            assertThat(review.getRating()).isEqualTo(5);
            assertThat(review.getContent()).isEqualTo("Amazing teacher!");
            assertThat(review.getTags()).containsExactly("professional", "patient");
            assertThat(review.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should handle null tags as empty list")
        void shouldHandleNullTags() {
            Review review = new Review(ReviewId.generate(), "booking-1",
                    "student-1", 100L, 3, "OK", null);
            assertThat(review.getTags()).isEmpty();
        }

        @Test
        @DisplayName("should accept empty content")
        void shouldAcceptEmptyContent() {
            Review review = new Review(ReviewId.generate(), "booking-1",
                    "student-1", 100L, 4, "", List.of());
            assertThat(review.getContent()).isEmpty();
        }
    }
}
