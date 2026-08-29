package com.neopick.application.booking;

import com.neopick.domain.booking.Booking;
import com.neopick.domain.booking.BookingRepository;
import com.neopick.domain.booking.BookingStatus;
import com.neopick.port.security.SecurityContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetStudentBookingsUseCase {

    private final BookingRepository bookingRepository;
    private final SecurityContext securityContext;

    public GetStudentBookingsUseCase(BookingRepository bookingRepository, SecurityContext securityContext) {
        this.bookingRepository = bookingRepository;
        this.securityContext = securityContext;
    }

    public SearchResult execute(String status, int page, int size) {
        String userId = securityContext.requireCurrentUserId();
        BookingStatus bookingStatus = status != null ? BookingStatus.valueOf(status) : null;
        List<Booking> bookings = bookingRepository.findByStudentId(userId, bookingStatus, page, size);
        long total = bookingRepository.countByStudentId(userId, bookingStatus);
        return new SearchResult(bookings, total);
    }

    public record SearchResult(List<Booking> bookings, long total) {}
}
