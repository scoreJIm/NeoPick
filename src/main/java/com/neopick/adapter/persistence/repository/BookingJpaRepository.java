package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.BookingJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingJpaRepository extends JpaRepository<BookingJpaEntity, UUID> {

    List<BookingJpaEntity> findByStudentIdAndStatus(String studentId, String status, Pageable pageable);

    List<BookingJpaEntity> findByTeacherIdAndStatus(Long teacherId, String status, Pageable pageable);

    long countByStudentIdAndStatus(String studentId, String status);

    long countByTeacherIdAndStatus(Long teacherId, String status);
}
