package com.neopick.adapter.web.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Request to submit a new lesson booking")
public record SubmitBookingRequest(
        @NotNull
        @Schema(description = "ID of the teacher to book", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
        Long teacherId,

        @NotNull
        @Schema(description = "Scheduled start time of the lesson", example = "2024-06-15T14:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime scheduledStart,

        @NotNull
        @Schema(description = "Lesson duration in minutes", example = "60", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer durationMinutes,

        @NotNull
        @Schema(description = "Agreed lesson price in CNY", example = "200.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal price,

        @NotNull
        @Schema(description = "Address label (e.g., home, studio name)", example = "My Home", requiredMode = Schema.RequiredMode.REQUIRED)
        String addressLabel,

        @Schema(description = "Detailed address", example = "Room 1201, Building 3, 888 Huaihai Road", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String addressDetail,

        @Schema(description = "GPS latitude of the lesson location", example = "31.2304", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        double latitude,

        @Schema(description = "GPS longitude of the lesson location", example = "121.4737", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        double longitude,

        @Schema(description = "Optional note from student to teacher", example = "Please bring an extra guitar pick", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String note
) {}
