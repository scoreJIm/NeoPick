package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.NotificationJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    List<NotificationJpaEntity> findByUserIdAndTypeOrderByCreatedAtDesc(
            String userId, String type, Pageable pageable);

    List<NotificationJpaEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserIdAndReadFalse(String userId);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    void markAllAsRead(String userId);
}
