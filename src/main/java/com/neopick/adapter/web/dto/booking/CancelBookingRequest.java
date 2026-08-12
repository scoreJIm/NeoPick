package com.neopick.adapter.web.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to cancel or reject a booking with a reason")
public record CancelBookingRequest(
        @NotBlank
        @Schema(description = "Reason for cancellation or rejection", example = "Schedule conflict with another appointment", requiredMode = Schema.RequiredMode.REQUIRED)
        String reason
) {}
