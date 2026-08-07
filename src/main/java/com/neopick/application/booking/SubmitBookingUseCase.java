package com.neopick.application.booking;

import com.neopick.domain.booking.*;
import com.neopick.port.security.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class SubmitBookingUseCase {

    private final BookingRepository bookingRepository;
    private final SecurityContext securityContext;

    public SubmitBookingUseCase(BookingRepository bookingRepository, SecurityContext securityContext) {
        this.bookingRepository = bookingRepository;
        this.securityContext = securityContext;
    }

    @Transactional
    public Booking execute(SubmitBookingCommand command) {
        String studentId = securityContext.requireCurrentUserId();
        LocalDateTime end = command.scheduledStart().plusMinutes(command.durationMinutes());
        Booking booking = new Booking(
                BookingId.generate(), studentId, command.teacherId(),
                command.scheduledStart(), end, command.durationMinutes(),
                command.price(),
                new Address(command.addressLabel(), command.addressDetail(),
                        command.latitude(), command.longitude()),
                command.note());
        return bookingRepository.save(booking);
    }

    public record SubmitBookingCommand(
            Long teacherId, LocalDateTime scheduledStart, int durationMinutes,
            BigDecimal price, String addressLabel, String addressDetail,
            double latitude, double longitude, String note) {}
}
