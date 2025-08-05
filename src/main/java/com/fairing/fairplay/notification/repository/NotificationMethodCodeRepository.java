package com.fairing.fairplay.notification.repository;

import com.fairing.fairplay.notification.entity.NotificationMethodCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationMethodCodeRepository extends JpaRepository<NotificationMethodCode, Integer> {
    NotificationMethodCode findByCode(String code);
}
