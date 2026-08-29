package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.ReviewJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewJpaRepository extends JpaRepository<ReviewJpaEntity, UUID> {

    List<ReviewJpaEntity> findByTeacherIdOrderByCreatedAtDesc(Long teacherId, Pageable pageable);

    List<ReviewJpaEntity> findByStudentIdOrderByCreatedAtDesc(String studentId, Pageable pageable);

    boolean existsByBookingId(String bookingId);
}
