package com.fairing.fairplay.notification.repository;

import com.fairing.fairplay.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // 삭제되지 않은 알림만 조회
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndNotDeletedOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    // 삭제되지 않은 알림 단건 조회
    @Query("SELECT n FROM Notification n WHERE n.notificationId = :notificationId AND n.deletedAt IS NULL")
    Optional<Notification> findByIdAndNotDeleted(@Param("notificationId") Long notificationId);
    
    // 기존 메서드는 호환성을 위해 유지 (하지만 사용하지 않을 예정)
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
}
