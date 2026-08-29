package com.neopick.adapter.persistence.impl;

import com.neopick.adapter.persistence.entity.ReviewJpaEntity;
import com.neopick.adapter.persistence.repository.ReviewJpaRepository;
import com.neopick.domain.review.Review;
import com.neopick.domain.review.ReviewId;
import com.neopick.domain.review.ReviewRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class ReviewRepositoryImpl implements ReviewRepository {

    private final ReviewJpaRepository jpaRepository;

    public ReviewRepositoryImpl(ReviewJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Review save(Review review) {
        ReviewJpaEntity e = toEntity(review);
        return toDomain(jpaRepository.save(e));
    }

    @Override
    public Optional<Review> findById(ReviewId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Review> findByTeacherId(Long teacherId, int page, int size) {
        return jpaRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId, PageRequest.of(page, size))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Review> findByStudentId(String studentId, int page, int size) {
        return jpaRepository.findByStudentIdOrderByCreatedAtDesc(studentId, PageRequest.of(page, size))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByBookingId(String bookingId) {
        return jpaRepository.existsByBookingId(bookingId);
    }

    private ReviewJpaEntity toEntity(Review r) {
        ReviewJpaEntity e = new ReviewJpaEntity();
        e.setId(r.getId().value());
        e.setBookingId(r.getBookingId());
        e.setStudentId(r.getStudentId());
        e.setTeacherId(r.getTeacherId());
        e.setRating(r.getRating());
        e.setContent(r.getContent());
        e.setTags(r.getTags() != null ? String.join(",", r.getTags()) : null);
        e.setCreatedAt(r.getCreatedAt());
        return e;
    }

    private Review toDomain(ReviewJpaEntity e) {
        List<String> tags = e.getTags() != null && !e.getTags().isEmpty()
                ? Arrays.asList(e.getTags().split(",")) : List.of();
        return new Review(new ReviewId(e.getId()), e.getBookingId(),
                e.getStudentId(), e.getTeacherId(), e.getRating(), e.getContent(), tags);
    }
}
