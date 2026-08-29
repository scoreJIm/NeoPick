package com.neopick.adapter.persistence.impl;

import com.neopick.adapter.persistence.entity.TeacherJpaEntity;
import com.neopick.adapter.persistence.repository.TeacherJpaRepository;
import com.neopick.adapter.persistence.spec.TeacherSpecification;
import com.neopick.domain.teacher.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class TeacherRepositoryImpl implements TeacherRepository {

    private final TeacherJpaRepository jpaRepository;

    public TeacherRepositoryImpl(TeacherJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Teacher save(Teacher teacher) {
        TeacherJpaEntity entity = toEntity(teacher);
        TeacherJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Teacher> findById(TeacherId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Teacher> search(TeacherSearchCriteria criteria) {
        Specification<TeacherJpaEntity> spec = TeacherSpecification.withCriteria(
                criteria.cityCode(), criteria.categoryId(), criteria.gender(),
                criteria.level(), criteria.priceMin(), criteria.priceMax(), criteria.keyword());
        Sort sort = resolveSort(criteria.sort());
        PageRequest pageRequest = PageRequest.of(criteria.page(), criteria.size(), sort);
        return jpaRepository.findAll(spec, pageRequest)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public long count(TeacherSearchCriteria criteria) {
        Specification<TeacherJpaEntity> spec = TeacherSpecification.withCriteria(
                criteria.cityCode(), criteria.categoryId(), criteria.gender(),
                criteria.level(), criteria.priceMin(), criteria.priceMax(), criteria.keyword());
        return jpaRepository.count(spec);
    }

    @Override
    public List<Teacher> findFeatured(String cityCode, int limit) {
        List<TeacherJpaEntity> entities = jpaRepository.findFeaturedByCity(cityCode);
        return entities.stream().limit(limit).map(this::toDomain).toList();
    }

    @Override
    public List<Teacher> findPopular(String cityCode, int limit) {
        List<TeacherJpaEntity> entities = jpaRepository.findPopularByCity(cityCode);
        return entities.stream().limit(limit).map(this::toDomain).toList();
    }

    private Sort resolveSort(String sort) {
        if (sort == null) return Sort.by(Sort.Direction.DESC, "bookingCount");
        return switch (sort) {
            case "PRICE_ASC" -> Sort.by(Sort.Direction.ASC, "basePrice");
            case "PRICE_DESC" -> Sort.by(Sort.Direction.DESC, "basePrice");
            case "RATING_ASC" -> Sort.by(Sort.Direction.ASC, "rating");
            case "RATING_DESC" -> Sort.by(Sort.Direction.DESC, "rating");
            default -> Sort.by(Sort.Direction.DESC, "bookingCount");
        };
    }

    private TeacherJpaEntity toEntity(Teacher teacher) {
        TeacherJpaEntity e = new TeacherJpaEntity();
        e.setId(teacher.getId() != null ? teacher.getId().value() : null);
        e.setUserId(teacher.getUserId());
        e.setRealName(teacher.getRealName());
        e.setBio(teacher.getBio());
        e.setLevel(teacher.getLevel().name());
        e.setTeachingYears(teacher.getTeachingYears());
        e.setBasePrice(teacher.getBasePrice());
        e.setCityCode(teacher.getCity().code());
        e.setCityName(teacher.getCity().name());
        e.setDistrict(teacher.getDistrict());
        e.setCoverImageUrl(teacher.getCoverImageUrl());
        e.setRating(teacher.getRating());
        e.setReviewCount(teacher.getReviewCount());
        e.setBookingCount(teacher.getBookingCount());
        e.setFeatured(teacher.isFeatured());
        e.setStatus(teacher.getStatus().name());
        e.setCreatedAt(teacher.getCreatedAt());
        e.setUpdatedAt(teacher.getUpdatedAt());
        return e;
    }

    private Teacher toDomain(TeacherJpaEntity e) {
        Teacher t = new Teacher(
                new TeacherId(e.getId()), e.getUserId(), e.getRealName(),
                TeacherLevel.valueOf(e.getLevel()), e.getBasePrice(),
                new City(e.getCityCode(), e.getCityName()), e.getDistrict());
        return t;
    }
}
