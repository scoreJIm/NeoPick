package com.neopick.adapter.web.controller;

import com.neopick.application.review.GetMyReviewsUseCase;
import com.neopick.application.review.SubmitReviewUseCase;
import com.neopick.domain.review.Review;
import com.neopick.domain.review.ReviewId;
import com.neopick.domain.review.ReviewRepository;
import com.neopick.port.security.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import({SubmitReviewUseCase.class, GetMyReviewsUseCase.class})
@DisplayName("Review API Integration Tests")
class ReviewControllerIT {

    @Autowired private MockMvc mockMvc;

    @MockBean private ReviewRepository reviewRepository;
    @MockBean private SecurityContext securityContext;

    private static final String STUDENT_ID = "student-001";
    private static final Long TEACHER_ID = 100L;
    private static final String BOOKING_ID = UUID.randomUUID().toString();
    private static final UUID REVIEW_UUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(securityContext.requireCurrentUserId()).thenReturn(STUDENT_ID);
        when(securityContext.getCurrentUserId()).thenReturn(Optional.of(STUDENT_ID));
    }

    @Nested
    @DisplayName("POST /api/v1/reviews — Submit review")
    class SubmitReview {

        @Test
        @DisplayName("should submit a review with valid data")
        void shouldSubmitReview() throws Exception {
            when(reviewRepository.existsByBookingId(BOOKING_ID)).thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "booking_id": "%s",
                                        "teacher_id": 100,
                                        "rating": 5,
                                        "content": "Great teacher, very patient!",
                                        "tags": ["patient", "professional"]
                                    }""".formatted(BOOKING_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.rating").value(5))
                    .andExpect(jsonPath("$.data.content").value("Great teacher, very patient!"))
                    .andExpect(jsonPath("$.data.tags", hasSize(2)))
                    .andExpect(jsonPath("$.data.teacher_id").value(100));
        }

        @Test
        @DisplayName("should reject duplicate review for same booking")
        void shouldRejectDuplicateReview() throws Exception {
            when(reviewRepository.existsByBookingId(BOOKING_ID)).thenReturn(true);

            mockMvc.perform(post("/api/v1/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "booking_id": "%s",
                                        "teacher_id": 100,
                                        "rating": 4,
                                        "content": "Second review attempt"
                                    }""".formatted(BOOKING_ID)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject rating below 1")
        void shouldRejectRatingBelow1() throws Exception {
            mockMvc.perform(post("/api/v1/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "booking_id": "%s",
                                        "teacher_id": 100,
                                        "rating": 0,
                                        "content": "Invalid rating"
                                    }""".formatted(BOOKING_ID)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject rating above 5")
        void shouldRejectRatingAbove5() throws Exception {
            mockMvc.perform(post("/api/v1/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "booking_id": "%s",
                                        "teacher_id": 100,
                                        "rating": 6,
                                        "content": "Invalid rating"
                                    }""".formatted(BOOKING_ID)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should reject review without booking_id")
        void shouldRejectMissingBookingId() throws Exception {
            mockMvc.perform(post("/api/v1/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"teacher_id": 100, "rating": 5}"""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should accept review with rating 1 (minimum valid)")
        void shouldAcceptMinimumRating() throws Exception {
            when(reviewRepository.existsByBookingId(BOOKING_ID)).thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "booking_id": "%s",
                                        "teacher_id": 100,
                                        "rating": 1,
                                        "content": "Not satisfied"
                                    }""".formatted(BOOKING_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.rating").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/reviews/my — List my reviews")
    class GetMyReviews {

        @Test
        @DisplayName("should return user's reviews")
        void shouldReturnMyReviews() throws Exception {
            Review review = new Review(new ReviewId(REVIEW_UUID), BOOKING_ID,
                    STUDENT_ID, TEACHER_ID, 5, "Excellent!", List.of("pro"));
            when(reviewRepository.findByStudentId(STUDENT_ID, 0, 20)).thenReturn(List.of(review));

            mockMvc.perform(get("/api/v1/reviews/my")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].rating").value(5))
                    .andExpect(jsonPath("$.data[0].teacher_id").value(100));
        }

        @Test
        @DisplayName("should return empty list when no reviews")
        void shouldReturnEmptyList() throws Exception {
            when(reviewRepository.findByStudentId(STUDENT_ID, 0, 20)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/reviews/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }
    }
}
