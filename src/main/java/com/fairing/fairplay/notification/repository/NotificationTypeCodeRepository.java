package com.fairing.fairplay.notification.repository;

import com.fairing.fairplay.notification.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationTypeCodeRepository extends JpaRepository<NotificationTypeCode, Integer> {
    NotificationTypeCode findByCode(String code);
}

