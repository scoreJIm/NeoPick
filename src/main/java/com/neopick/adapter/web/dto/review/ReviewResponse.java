package com.neopick.adapter.web.dto.review;

import com.neopick.domain.review.Review;

import java.util.List;

public record ReviewResponse(
        String id,
        String bookingId,
        String studentId,
        Long teacherId,
        int rating,
        String content,
        List<String> tags,
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
