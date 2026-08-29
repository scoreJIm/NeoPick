package com.neopick.adapter.persistence.impl;

import com.neopick.adapter.persistence.entity.UserJpaEntity;
import com.neopick.adapter.persistence.repository.UserJpaRepository;
import com.neopick.domain.user.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryImpl(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<User> findByPhone(PhoneNumber phone) {
        return jpaRepository.findByPhone(phone.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByPhone(PhoneNumber phone) {
        return jpaRepository.existsByPhone(phone.value());
    }

    private UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId().value());
        entity.setPhone(user.getPhone().value());
        entity.setNickname(user.getNickname());
        entity.setAvatarUrl(user.getAvatarUrl());
        entity.setGender(user.getGender() != null ? user.getGender().name() : null);
        entity.setRole(user.getRole().name());
        entity.setStatus(user.getStatus().name());
        entity.setRegisteredAt(user.getRegisteredAt());
        entity.setUpdatedAt(user.getUpdatedAt() != null ? user.getUpdatedAt() : LocalDateTime.now());
        entity.setLastLoginAt(user.getLastLoginAt());
        return entity;
    }

    private User toDomain(UserJpaEntity entity) {
        return User.reconstruct(
                new UserId(entity.getId()),
                PhoneNumber.of(entity.getPhone()),
                entity.getNickname(),
                entity.getAvatarUrl(),
                entity.getGender() != null ? Gender.valueOf(entity.getGender()) : null,
                UserRole.valueOf(entity.getRole()),
                UserStatus.valueOf(entity.getStatus()),
                entity.getRegisteredAt(),
                entity.getUpdatedAt(),
                entity.getLastLoginAt());
    }
}
