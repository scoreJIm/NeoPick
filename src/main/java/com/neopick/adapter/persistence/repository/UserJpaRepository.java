package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByPhone(String phone);

    boolean existsByPhone(String phone);
}
