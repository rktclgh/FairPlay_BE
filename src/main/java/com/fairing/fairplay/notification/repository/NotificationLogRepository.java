package com.fairing.fairplay.notification.repository;

import com.fairing.fairplay.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByNotification_NotificationId(Long notificationId);
}
