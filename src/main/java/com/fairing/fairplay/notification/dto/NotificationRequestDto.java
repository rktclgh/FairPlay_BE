package com.fairing.fairplay.notification.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationRequestDto {
    private Long userId;
    private String typeCode;      // 예: "RESERVATION"
    private String methodCode;    // 예: "EMAIL" 또는 "WEB"
    private String title;
    private String message;
    private String url;
}
