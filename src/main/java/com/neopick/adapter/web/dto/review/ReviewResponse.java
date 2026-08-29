package com.neopick.adapter.web.dto.review;

import com.neopick.domain.review.Review;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Review details including rating and tags")
public record ReviewResponse(
        @Schema(description = "Review ID (UUID)", example = "770e8400-e29b-41d4-a716-446655440002")
        String id,

        @Schema(description = "Associated booking ID", example = "550e8400-e29b-41d4-a716-446655440000")
        String bookingId,

        @Schema(description = "Student who wrote the review", example = "user_student_001")
        String studentId,

        @Schema(description = "Teacher being reviewed", example = "42")
        Long teacherId,

        @Schema(description = "Rating from 1 to 5", example = "5")
        int rating,

        @Schema(description = "Written review content", example = "Great teacher!")
        String content,

        @Schema(description = "Review tags", example = "[\"Patient\", \"Professional\"]")
        List<String> tags,

        @Schema(description = "Review creation timestamp", example = "2024-06-15T16:00:00")
        String createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId().value().toString(),
                review.getBookingId(),
                review.getStudentId(),
                review.getTeacherId(),
                review.getRating(),
                review.getContent(),
                review.getTags(),
                review.getCreatedAt() != null ? review.getCreatedAt().toString() : null
        );
    }
}
