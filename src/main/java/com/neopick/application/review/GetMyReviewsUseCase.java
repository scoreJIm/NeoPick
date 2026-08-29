package com.neopick.application.review;

import com.neopick.domain.review.Review;
import com.neopick.domain.review.ReviewRepository;
import com.neopick.port.security.SecurityContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetMyReviewsUseCase {

    private final ReviewRepository reviewRepository;
    private final SecurityContext securityContext;

    public GetMyReviewsUseCase(ReviewRepository reviewRepository, SecurityContext securityContext) {
        this.reviewRepository = reviewRepository;
        this.securityContext = securityContext;
    }

    public List<Review> execute(int page, int size) {
        String userId = securityContext.requireCurrentUserId();
        return reviewRepository.findByStudentId(userId, page, size);
    }
}
