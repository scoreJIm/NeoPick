package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.TeacherJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TeacherJpaRepository extends JpaRepository<TeacherJpaEntity, Long>,
        JpaSpecificationExecutor<TeacherJpaEntity> {

    @Query("SELECT t FROM TeacherJpaEntity t WHERE t.featured = true AND t.cityCode = :cityCode " +
           "AND t.status = 'APPROVED' ORDER BY t.bookingCount DESC")
    List<TeacherJpaEntity> findFeaturedByCity(String cityCode);

    @Query("SELECT t FROM TeacherJpaEntity t WHERE t.cityCode = :cityCode " +
           "AND t.status = 'APPROVED' ORDER BY t.bookingCount DESC")
    List<TeacherJpaEntity> findPopularByCity(String cityCode);
}
