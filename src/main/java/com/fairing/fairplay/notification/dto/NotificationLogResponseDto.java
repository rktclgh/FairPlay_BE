package com.fairing.fairplay.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class NotificationLogResponseDto {
    private Long logId;
    private String methodCode;
    private String status;
    private Boolean isSent;
    private String detail;
    private LocalDateTime sentAt;
}
