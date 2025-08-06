package com.fairing.fairplay.notification.service;

import com.fairing.fairplay.notification.entity.*;
import com.fairing.fairplay.notification.repository.*;
import com.fairing.fairplay.notification.dto.*;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTypeCodeRepository typeCodeRepository;
    private final NotificationMethodCodeRepository methodCodeRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final com.fairing.fairplay.notification.service.NotificationEmailService notificationEmailService;
    private final UserRepository userRepository;

    // 알림 생성 (이메일 발송+로그)
    @Transactional
    public NotificationResponseDto createNotification(NotificationRequestDto dto) {
        NotificationTypeCode typeCode = typeCodeRepository.findByCode(dto.getTypeCode());
        NotificationMethodCode methodCode = methodCodeRepository.findByCode(dto.getMethodCode());
        if (typeCode == null || methodCode == null)
            throw new IllegalArgumentException("유효하지 않은 typeCode or methodCode");

        Notification notification = Notification.builder()
                .userId(dto.getUserId())
                .typeCode(typeCode)
                .methodCode(methodCode)
                .title(dto.getTitle())
                .message(dto.getMessage())
                .url(dto.getUrl())
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        // EMAIL이면 메일 발송
        if ("EMAIL".equalsIgnoreCase(dto.getMethodCode())) {
            Users user = userRepository.findByUserId(dto.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
            notificationEmailService.sendEmailNotification(
                    notification.getNotificationId(),
                    user.getEmail(),
                    dto.getTitle(),
                    dto.getMessage(),
                    dto.getUrl()
            );
        }
        return toResponseDto(notification);
    }

    // 내 알림 리스트
    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    // 내 알림만 읽음 처리
    @Transactional
    public void markAsReadByUser(Long notificationId, Long userId) {
        Notification notification = getMyNotification(notificationId, userId);
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    // 내 알림만 상세조회
    @Transactional(readOnly = true)
    public NotificationResponseDto getNotificationByUser(Long notificationId, Long userId) {
        return toResponseDto(getMyNotification(notificationId, userId));
    }

    // 내 알림만 삭제
    @Transactional
    public void deleteNotificationByUser(Long notificationId, Long userId) {
        Notification notification = getMyNotification(notificationId, userId);
        notificationRepository.deleteById(notificationId);
    }

    // 내 알림 여러개 일괄 삭제
    @Transactional
    public void deleteNotificationsByUser(List<Long> notificationIds, Long userId) {
        for (Long id : notificationIds) {
            deleteNotificationByUser(id, userId);
        }
    }

    // 내 알림만 로그 조회 - 반환 타입 변경
    @Transactional(readOnly = true)
    public List<NotificationLogResponseDto> getLogsByUser(Long notificationId, Long userId) {
        Notification notification = getMyNotification(notificationId, userId);
        return notificationLogRepository.findByNotification_NotificationId(notification.getNotificationId())
                .stream()
                .map(this::toLogResponseDto)
                .collect(Collectors.toList());
    }

    // 변환 메서드 추가
    private NotificationLogResponseDto toLogResponseDto(NotificationLog log) {
        return NotificationLogResponseDto.builder()
                .logId(log.getLogId())
                .methodCode(log.getMethodCode().getCode())
                .status(log.getStatus())
                .isSent(log.getIsSent())
                .detail(log.getDetail())
                .sentAt(log.getSentAt())
                .build();
    }

    // ========== 내부 공용 메서드 ==========

    // userId 검증 후 알림 리턴(내 알림만 접근 가능)
    private Notification getMyNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림이 존재하지 않습니다."));
        if (!notification.getUserId().equals(userId)) {
            throw new SecurityException("본인 알림만 접근/삭제 가능합니다.");
        }
        return notification;
    }

    // 변환
    private NotificationResponseDto toResponseDto(Notification n) {
        return NotificationResponseDto.builder()
                .notificationId(n.getNotificationId())
                .typeCode(n.getTypeCode().getCode())
                .methodCode(n.getMethodCode().getCode())
                .title(n.getTitle())
                .message(n.getMessage())
                .url(n.getUrl())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    // 이거 끌아다가 쓰시면 됩니다!!!!!!
    //웹 알림
    public static NotificationRequestDto buildWebNotification(Long userId, String typeCode, String title, String message, String url) {
        return NotificationRequestDto.builder()
                .userId(userId)
                .typeCode(typeCode)
                .methodCode("WEB")
                .title(title)
                .message(message)
                .url(url)
                .build();
    }
    //이메일 발송!
    public static NotificationRequestDto buildEmailNotification(Long userId, String typeCode, String title, String message, String url) {
        return NotificationRequestDto.builder()
                .userId(userId)
                .typeCode(typeCode)
                .methodCode("EMAIL")
                .title(title)
                .message(message)
                .url(url)
                .build();
    }

}
