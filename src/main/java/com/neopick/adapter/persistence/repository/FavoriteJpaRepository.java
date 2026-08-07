package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.FavoriteJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteJpaRepository extends JpaRepository<FavoriteJpaEntity, Long> {

    List<FavoriteJpaEntity> findByStudentIdOrderByCreatedAtDesc(String studentId, Pageable pageable);

    boolean existsByStudentIdAndTeacherId(String studentId, Long teacherId);

    void deleteByStudentIdAndTeacherId(String studentId, Long teacherId);
}
