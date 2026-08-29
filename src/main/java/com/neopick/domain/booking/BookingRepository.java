package com.neopick.domain.booking;

import java.util.Optional;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(BookingId id);

    java.util.List<Booking> findByStudentId(String studentId, BookingStatus status, int page, int size);

    java.util.List<Booking> findByTeacherId(Long teacherId, BookingStatus status, int page, int size);

    long countByStudentId(String studentId, BookingStatus status);

    long countByTeacherId(Long teacherId, BookingStatus status);
}
