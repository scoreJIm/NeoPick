package com.neopick.adapter.persistence.impl;

import com.neopick.adapter.persistence.entity.FavoriteJpaEntity;
import com.neopick.adapter.persistence.repository.FavoriteJpaRepository;
import com.neopick.domain.favorite.Favorite;
import com.neopick.domain.favorite.FavoriteRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class FavoriteRepositoryImpl implements FavoriteRepository {

    private final FavoriteJpaRepository jpaRepository;

    public FavoriteRepositoryImpl(FavoriteJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Favorite save(Favorite favorite) {
        FavoriteJpaEntity e = new FavoriteJpaEntity();
        e.setStudentId(favorite.getStudentId());
        e.setTeacherId(favorite.getTeacherId());
        e.setCreatedAt(favorite.getCreatedAt());
        FavoriteJpaEntity saved = jpaRepository.save(e);
        return new Favorite(saved.getStudentId(), saved.getTeacherId());
    }

    @Override
    @Transactional
    public void delete(String studentId, Long teacherId) {
        jpaRepository.deleteByStudentIdAndTeacherId(studentId, teacherId);
    }

    @Override
    public List<Favorite> findByStudentId(String studentId, int page, int size) {
        return jpaRepository.findByStudentIdOrderByCreatedAtDesc(studentId, PageRequest.of(page, size))
                .stream().map(e -> new Favorite(e.getStudentId(), e.getTeacherId())).toList();
    }

    @Override
    public boolean exists(String studentId, Long teacherId) {
        return jpaRepository.existsByStudentIdAndTeacherId(studentId, teacherId);
    }
}
