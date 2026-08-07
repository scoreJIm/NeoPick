package com.neopick.adapter.web.dto.booking;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubmitBookingRequest(
        @NotNull Long teacherId,
        @NotNull LocalDateTime scheduledStart,
        @NotNull Integer durationMinutes,
        @NotNull BigDecimal price,
        @NotNull String addressLabel,
        String addressDetail,
        double latitude,
        double longitude,
        String note
) {}
