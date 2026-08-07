package com.neopick.adapter.web.dto.booking;

import com.neopick.domain.booking.Booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingResponse(
        String id,
        String status,
        String studentId,
        Long teacherId,
        LocalDateTime scheduledStart,
        LocalDateTime scheduledEnd,
        int durationMinutes,
        BigDecimal price,
        AddressResponse address,
        String studentNote,
        String cancelReason,
        String cancelledBy,
        LocalDateTime confirmedAt,
        LocalDateTime paidAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt
) {
    public record AddressResponse(
            String label, String detail, double latitude, double longitude) {}

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
