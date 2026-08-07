package com.neopick.domain.review;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository {

    Review save(Review review);

    Optional<Review> findById(ReviewId id);

    List<Review> findByTeacherId(Long teacherId, int page, int size);

    List<Review> findByStudentId(String studentId, int page, int size);

    boolean existsByBookingId(String bookingId);
}
