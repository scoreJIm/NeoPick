package com.neopick.application.review;

import com.neopick.domain.review.Review;
import com.neopick.domain.review.ReviewId;
import com.neopick.domain.review.ReviewRepository;
import com.neopick.infrastructure.metrics.BusinessMetrics;
import com.neopick.port.security.SecurityContext;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubmitReviewUseCase {

    private final ReviewRepository reviewRepository;
    private final SecurityContext securityContext;
    private final BusinessMetrics metrics;

    public SubmitReviewUseCase(ReviewRepository reviewRepository, SecurityContext securityContext,
                               BusinessMetrics metrics) {
        this.reviewRepository = reviewRepository;
        this.securityContext = securityContext;
        this.metrics = metrics;
    }

    @Transactional
    @CacheEvict(value = "teacherDetail", key = "#command.teacherId()")
    public Review execute(SubmitReviewCommand command) {
        if (reviewRepository.existsByBookingId(command.bookingId())) {
            throw new IllegalStateException("Review already exists for this booking");
        }
        String studentId = securityContext.requireCurrentUserId();
        Review review = new Review(ReviewId.generate(), command.bookingId(),
                studentId, command.teacherId(), command.rating(),
                command.content(), command.tags());
        Review saved = reviewRepository.save(review);
        metrics.reviewSubmitted();
        return saved;
    }

    public record SubmitReviewCommand(
            String bookingId, Long teacherId, int rating, String content, List<String> tags) {}
}
