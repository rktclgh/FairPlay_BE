package com.fairing.fairplay.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatRoomResponseDto {
    private Long chatRoomId;
    private Long eventId; // null이면 전체관리자 문의
    private Long userId;
    private String targetType;
    private Long targetId;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
    private String eventTitle; // 이벤트 제목 추가
    private String userName; // 사용자 이름 추가
    private Long unreadCount; // 안 읽은 메시지 수
}
