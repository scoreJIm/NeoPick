package com.neopick.domain.notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(NotificationId id);

    List<Notification> findByUserId(String userId, NotificationType type, int page, int size);

    long countByUserIdAndReadFalse(String userId);

    void markAllAsRead(String userId);
}
