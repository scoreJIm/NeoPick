package com.neopick.adapter.web.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record SubmitReviewRequest(
        @NotBlank String bookingId,
        @NotBlank Long teacherId,
        @Min(1) @Max(5) int rating,
        String content,
        List<String> tags
) {}
