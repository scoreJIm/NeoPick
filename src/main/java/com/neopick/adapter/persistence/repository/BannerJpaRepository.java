package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.BannerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BannerJpaRepository extends JpaRepository<BannerJpaEntity, Long> {

    @Query("SELECT b FROM BannerJpaEntity b WHERE b.active = true AND " +
           "(b.cityCode = :cityCode OR b.cityCode IS NULL) ORDER BY b.sortOrder ASC")
    List<BannerJpaEntity> findActiveByCity(String cityCode);
}
