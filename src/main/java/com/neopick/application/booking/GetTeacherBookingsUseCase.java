package com.neopick.application.booking;

import com.neopick.domain.booking.Booking;
import com.neopick.domain.booking.BookingRepository;
import com.neopick.domain.booking.BookingStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetTeacherBookingsUseCase {

    private final BookingRepository bookingRepository;

    public GetTeacherBookingsUseCase(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public SearchResult execute(Long teacherId, String status, int page, int size) {
        BookingStatus bookingStatus = status != null ? BookingStatus.valueOf(status) : null;
        List<Booking> bookings = bookingRepository.findByTeacherId(teacherId, bookingStatus, page, size);
        long total = bookingRepository.countByTeacherId(teacherId, bookingStatus);
        return new SearchResult(bookings, total);
    }

    public record SearchResult(List<Booking> bookings, long total) {}
}
