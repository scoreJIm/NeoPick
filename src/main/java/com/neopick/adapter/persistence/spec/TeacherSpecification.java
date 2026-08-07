package com.neopick.adapter.persistence.spec;

import com.neopick.adapter.persistence.entity.TeacherJpaEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class TeacherSpecification {

    private TeacherSpecification() {
    }

    public static Specification<TeacherJpaEntity> withCriteria(String cityCode, Long categoryId,
                                                                String gender, String level,
                                                                Double priceMin, Double priceMax,
                                                                String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), "APPROVED"));

            if (cityCode != null && !cityCode.isBlank()) {
                predicates.add(cb.equal(root.get("cityCode"), cityCode));
            }
            if (level != null && !level.isBlank()) {
                predicates.add(cb.equal(root.get("level"), level));
            }
            if (priceMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), priceMin));
            }
            if (priceMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), priceMax));
            }
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("realName")),
                        "%" + keyword.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
