package com.fairing.fairplay.banner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserApplicationListDto {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private String bannerType;  // "HERO" or "SEARCH_TOP"
    private String title;
    private String imageUrl;
    private String linkUrl;
    private String status;  // "PENDING", "APPROVED", "REJECTED"
    private Integer totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<UserApplicationItemDto> items;
    private String rejectionReason;
}
