package com.neopick.application.review;

import com.neopick.domain.review.Review;
import com.neopick.domain.review.ReviewId;
import com.neopick.domain.review.ReviewRepository;
import com.neopick.port.security.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubmitReviewUseCase {

    private final ReviewRepository reviewRepository;
    private final SecurityContext securityContext;

    public SubmitReviewUseCase(ReviewRepository reviewRepository, SecurityContext securityContext) {
        this.reviewRepository = reviewRepository;
        this.securityContext = securityContext;
    }

    @Transactional
    public Review execute(SubmitReviewCommand command) {
        if (reviewRepository.existsByBookingId(command.bookingId())) {
            throw new IllegalStateException("Review already exists for this booking");
        }
        String studentId = securityContext.requireCurrentUserId();
        Review review = new Review(ReviewId.generate(), command.bookingId(),
                studentId, command.teacherId(), command.rating(),
                command.content(), command.tags());
        return reviewRepository.save(review);
    }

    public record SubmitReviewCommand(
            String bookingId, Long teacherId, int rating, String content, List<String> tags) {}
}
