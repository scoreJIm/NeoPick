package com.neopick.application.booking;

import com.neopick.domain.booking.Booking;
import com.neopick.domain.booking.BookingId;
import com.neopick.domain.booking.BookingRepository;
import com.neopick.infrastructure.metrics.BusinessMetrics;
import com.neopick.port.security.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageBookingUseCase {

    private final BookingRepository bookingRepository;
    private final SecurityContext securityContext;
    private final BusinessMetrics metrics;

    public ManageBookingUseCase(BookingRepository bookingRepository, SecurityContext securityContext,
                                 BusinessMetrics metrics) {
        this.bookingRepository = bookingRepository;
        this.securityContext = securityContext;
        this.metrics = metrics;
    }

    private Booking getBooking(String bookingId) {
        return bookingRepository.findById(BookingId.from(bookingId))
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
    }

    @Transactional
    public Booking confirm(String bookingId) {
        Booking booking = getBooking(bookingId);
        booking.confirm();
        Booking saved = bookingRepository.save(booking);
        metrics.bookingConfirmed();
        return saved;
    }

    @Transactional
    public Booking reject(String bookingId, String reason) {
        Booking booking = getBooking(bookingId);
        booking.reject(reason);
        Booking saved = bookingRepository.save(booking);
        metrics.bookingCancelled();
        return saved;
    }

    @Transactional
    public Booking cancel(String bookingId, String reason) {
        String userId = securityContext.requireCurrentUserId();
        Booking booking = getBooking(bookingId);
        booking.cancel(reason, userId);
        Booking saved = bookingRepository.save(booking);
        metrics.bookingCancelled();
        return saved;
    }

    @Transactional
    public Booking complete(String bookingId) {
        Booking booking = getBooking(bookingId);
        booking.complete();
        Booking saved = bookingRepository.save(booking);
        metrics.bookingCompleted();
        return saved;
    }

    public Booking getDetail(String bookingId) {
        return getBooking(bookingId);
    }
}
