package com.neopick.adapter.web.dto.booking;

import jakarta.validation.constraints.NotBlank;

public record CancelBookingRequest(@NotBlank String reason) {}
