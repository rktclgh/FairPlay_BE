package com.fairing.fairplay.notification.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationResponseDto {
    private Long notificationId;
    private String typeCode;
    private String methodCode;
    private String title;
    private String message;
    private String url;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
