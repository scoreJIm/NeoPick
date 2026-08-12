package com.neopick.adapter.web.dto.booking;

import com.neopick.domain.booking.Booking;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Full booking details including status, schedule, and address")
public record BookingResponse(
        @Schema(description = "Booking ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "Booking status (PENDING, CONFIRMED, PAID, COMPLETED, CANCELLED, REJECTED)", example = "CONFIRMED")
        String status,

        @Schema(description = "Student user ID", example = "user_student_001")
        String studentId,

        @Schema(description = "Teacher ID", example = "42")
        Long teacherId,

        @Schema(description = "Scheduled lesson start time", example = "2024-06-15T14:00:00")
        LocalDateTime scheduledStart,

        @Schema(description = "Scheduled lesson end time", example = "2024-06-15T15:00:00")
        LocalDateTime scheduledEnd,

        @Schema(description = "Lesson duration in minutes", example = "60")
        int durationMinutes,

        @Schema(description = "Lesson price in CNY", example = "200.00")
        BigDecimal price,

        @Schema(description = "Lesson address details")
        AddressResponse address,

        @Schema(description = "Student's note to the teacher", example = "Please bring extra picks")
        String studentNote,

        @Schema(description = "Reason for cancellation or rejection", example = "Schedule conflict")
        String cancelReason,

        @Schema(description = "Who cancelled (STUDENT or TEACHER)", example = "STUDENT")
        String cancelledBy,

        @Schema(description = "Confirmation timestamp", example = "2024-06-14T09:00:00")
        LocalDateTime confirmedAt,

        @Schema(description = "Payment timestamp", example = "2024-06-14T09:15:00")
        LocalDateTime paidAt,

        @Schema(description = "Lesson completion timestamp", example = "2024-06-15T15:05:00")
        LocalDateTime completedAt,

        @Schema(description = "Cancellation timestamp", example = "2024-06-15T10:00:00")
        LocalDateTime cancelledAt,

        @Schema(description = "Booking creation timestamp", example = "2024-06-13T20:30:00")
        LocalDateTime createdAt
) {
    @Schema(description = "Address details for the lesson location")
    public record AddressResponse(
            @Schema(description = "Address label", example = "My Home") String label,
            @Schema(description = "Detailed address", example = "Room 1201, Building 3") String detail,
            @Schema(description = "GPS latitude", example = "31.2304") double latitude,
            @Schema(description = "GPS longitude", example = "121.4737") double longitude
    ) {}

    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId().value().toString(),
                booking.getStatus().name(),
                booking.getStudentId(),
                booking.getTeacherId(),
                booking.getScheduledStart(),
                booking.getScheduledEnd(),
                booking.getDurationMinutes(),
                booking.getPrice(),
                booking.getAddress() != null ? new AddressResponse(
                        booking.getAddress().label(),
                        booking.getAddress().detail(),
                        booking.getAddress().latitude(),
                        booking.getAddress().longitude()) : null,
                booking.getStudentNote(),
                booking.getCancelReason(),
                booking.getCancelledBy(),
                booking.getConfirmedAt(),
                booking.getPaidAt(),
                booking.getCompletedAt(),
                booking.getCancelledAt(),
                booking.getCreatedAt()
        );
    }
}
