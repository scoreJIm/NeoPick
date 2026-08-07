package com.neopick.adapter.persistence.impl;

import com.neopick.adapter.persistence.entity.NotificationJpaEntity;
import com.neopick.adapter.persistence.repository.NotificationJpaRepository;
import com.neopick.domain.notification.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class NotificationRepositoryImpl implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;

    public NotificationRepositoryImpl(NotificationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Notification save(Notification notification) {
        return toDomain(jpaRepository.save(toEntity(notification)));
    }

    @Override
    public Optional<Notification> findById(NotificationId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Notification> findByUserId(String userId, NotificationType type, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        List<NotificationJpaEntity> entities;
        if (type != null) {
            entities = jpaRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
                    userId, type.name(), pageable);
        } else {
            entities = jpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public long countByUserIdAndReadFalse(String userId) {
        return jpaRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAllAsRead(String userId) {
        jpaRepository.markAllAsRead(userId);
    }

    private NotificationJpaEntity toEntity(Notification n) {
        NotificationJpaEntity e = new NotificationJpaEntity();
        e.setId(n.getId().value());
        e.setUserId(n.getUserId());
        e.setTitle(n.getTitle());
        e.setContent(n.getContent());
        e.setType(n.getType().name());
        e.setReferenceId(n.getReferenceId());
        e.setRead(n.isRead());
        e.setCreatedAt(n.getCreatedAt());
        return e;
    }

    private Notification toDomain(NotificationJpaEntity e) {
        return new Notification(new NotificationId(e.getId()), e.getUserId(),
                e.getTitle(), e.getContent(), NotificationType.valueOf(e.getType()),
                e.getReferenceId());
    }
}
