package com.neopick.application.notification;

import com.neopick.domain.notification.Notification;
import com.neopick.domain.notification.NotificationRepository;
import com.neopick.domain.notification.NotificationType;
import com.neopick.port.security.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationUseCase {

    private final NotificationRepository notificationRepository;
    private final SecurityContext securityContext;

    public NotificationUseCase(NotificationRepository notificationRepository,
                               SecurityContext securityContext) {
        this.notificationRepository = notificationRepository;
        this.securityContext = securityContext;
    }

    public List<Notification> list(String typeStr, int page, int size) {
        String userId = securityContext.requireCurrentUserId();
        NotificationType type = typeStr != null ? NotificationType.valueOf(typeStr) : null;
        return notificationRepository.findByUserId(userId, type, page, size);
    }

    @Transactional
    public void markAsRead(String id) {
        notificationRepository.findById(
                        com.neopick.domain.notification.NotificationId.from(id))
                .ifPresent(n -> {
                    n.markAsRead();
                    notificationRepository.save(n);
                });
    }

    @Transactional
    public void markAllAsRead() {
        String userId = securityContext.requireCurrentUserId();
        notificationRepository.markAllAsRead(userId);
    }

    public long unreadCount() {
        String userId = securityContext.requireCurrentUserId();
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }
}
