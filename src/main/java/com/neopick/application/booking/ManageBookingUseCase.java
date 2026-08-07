package com.neopick.application.booking;

import com.neopick.domain.booking.Booking;
import com.neopick.domain.booking.BookingId;
import com.neopick.domain.booking.BookingRepository;
import com.neopick.port.security.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageBookingUseCase {

    private final BookingRepository bookingRepository;
    private final SecurityContext securityContext;

    public ManageBookingUseCase(BookingRepository bookingRepository, SecurityContext securityContext) {
        this.bookingRepository = bookingRepository;
        this.securityContext = securityContext;
    }

    private Booking getBooking(String bookingId) {
        return bookingRepository.findById(BookingId.from(bookingId))
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
    }

    @Transactional
    public Booking confirm(String bookingId) {
        Booking booking = getBooking(bookingId);
        booking.confirm();
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking reject(String bookingId, String reason) {
        Booking booking = getBooking(bookingId);
        booking.reject(reason);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking cancel(String bookingId, String reason) {
        String userId = securityContext.requireCurrentUserId();
        Booking booking = getBooking(bookingId);
        booking.cancel(reason, userId);
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking complete(String bookingId) {
        Booking booking = getBooking(bookingId);
        booking.complete();
        return bookingRepository.save(booking);
    }

    public Booking getDetail(String bookingId) {
        return getBooking(bookingId);
    }
}
