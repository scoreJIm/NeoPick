package com.neopick.adapter.web.dto.review;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Schema(description = "Request to submit a review after a completed lesson")
public record SubmitReviewRequest(
        @NotBlank
        @Schema(description = "Booking ID being reviewed", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
        String bookingId,

        @NotBlank
        @Schema(description = "Teacher ID being reviewed", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
        Long teacherId,

        @Min(1) @Max(5)
        @Schema(description = "Rating from 1 to 5", example = "5", minimum = "1", maximum = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        int rating,

        @Schema(description = "Written review content", example = "Great teacher! Very patient and knowledgeable.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String content,

        @Schema(description = "Tags describing the experience", example = "[\"Patient\", \"Professional\", \"Good with beginners\"]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<String> tags
) {}
