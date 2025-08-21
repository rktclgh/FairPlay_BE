package com.fairing.fairplay.banner.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminApplicationListItemDto {
    Long applicationId;
    String hostName;          // 없으면 빈 문자열
    Long eventId;
    String eventName;
    String bannerType;        // HERO | SEARCH_TOP
    LocalDateTime appliedAt;
    String applyStatus;       // PENDING | APPROVED | REJECTED
    String paymentStatus;     // WAITING | PAID | N/A ...
    String imageUrl;
    Integer totalAmount;
    List<AdminApplicationSlotDto> slots;
}
