package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.CityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CityJpaRepository extends JpaRepository<CityJpaEntity, String> {

    List<CityJpaEntity> findByIsHotTrueOrderBySortOrderAsc();
}
