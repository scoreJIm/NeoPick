package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, Long> {

    List<CategoryJpaEntity> findByActiveTrueOrderBySortOrderAsc();
}
