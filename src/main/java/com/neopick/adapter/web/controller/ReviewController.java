package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.review.ReviewResponse;
import com.neopick.adapter.web.dto.review.SubmitReviewRequest;
import com.neopick.application.review.GetMyReviewsUseCase;
import com.neopick.application.review.SubmitReviewUseCase;
import com.neopick.infrastructure.ratelimit.RateLimit;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@Tag(name = "Reviews", description = "Submit and retrieve lesson reviews")
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {

    private final SubmitReviewUseCase submitReviewUseCase;
    private final GetMyReviewsUseCase getMyReviewsUseCase;

    public ReviewController(SubmitReviewUseCase submitReviewUseCase,
                            GetMyReviewsUseCase getMyReviewsUseCase) {
        this.submitReviewUseCase = submitReviewUseCase;
        this.getMyReviewsUseCase = getMyReviewsUseCase;
    }

    @PostMapping
    @RateLimit(limit = 5, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.reviews.submit", description = "Submit review")
    @Operation(summary = "Submit a review", description = "Submits a review for a completed booking. Includes rating (1-5), optional content text, and tags.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review submitted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request — rating out of range or missing booking ID", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking or teacher not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Review already submitted for this booking", content = @Content)
    })
    public ApiResponse<ReviewResponse> submit(@Valid @RequestBody SubmitReviewRequest request) {
        var review = submitReviewUseCase.execute(new SubmitReviewUseCase.SubmitReviewCommand(
                request.bookingId(), request.teacherId(), request.rating(),
                request.content(), request.tags()));
        return ApiResponse.success(ReviewResponse.from(review));
    }

    @GetMapping("/my")
    @RateLimit(limit = 30, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.reviews.my", description = "Get my reviews")
    @Operation(summary = "Get my reviews", description = "Returns the authenticated student's submitted reviews, ordered by most recent first.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reviews returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ApiResponse<List<ReviewResponse>> myReviews(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        var reviews = getMyReviewsUseCase.execute(page, size);
        return ApiResponse.success(reviews.stream().map(ReviewResponse::from).toList());
    }
}
