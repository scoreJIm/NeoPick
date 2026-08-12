package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.review.ReviewResponse;
import com.neopick.adapter.web.dto.review.SubmitReviewRequest;
import com.neopick.application.review.GetMyReviewsUseCase;
import com.neopick.application.review.SubmitReviewUseCase;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final SubmitReviewUseCase submitReviewUseCase;
    private final GetMyReviewsUseCase getMyReviewsUseCase;

    public ReviewController(SubmitReviewUseCase submitReviewUseCase,
                            GetMyReviewsUseCase getMyReviewsUseCase) {
        this.submitReviewUseCase = submitReviewUseCase;
        this.getMyReviewsUseCase = getMyReviewsUseCase;
    }

    @PostMapping
    @Timed(value = "neopick.reviews.submit", description = "Submit review")
    public ApiResponse<ReviewResponse> submit(@Valid @RequestBody SubmitReviewRequest request) {
        var review = submitReviewUseCase.execute(new SubmitReviewUseCase.SubmitReviewCommand(
                request.bookingId(), request.teacherId(), request.rating(),
                request.content(), request.tags()));
        return ApiResponse.success(ReviewResponse.from(review));
    }

    @GetMapping("/my")
    @Timed(value = "neopick.reviews.my", description = "Get my reviews")
    public ApiResponse<List<ReviewResponse>> myReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var reviews = getMyReviewsUseCase.execute(page, size);
        return ApiResponse.success(reviews.stream().map(ReviewResponse::from).toList());
    }
}
