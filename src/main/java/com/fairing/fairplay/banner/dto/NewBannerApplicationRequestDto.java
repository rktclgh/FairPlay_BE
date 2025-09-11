package com.fairing.fairplay.banner.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class NewBannerApplicationRequestDto {
    private Long eventId;
    private String bannerType;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private LocalDate startDate;
    private LocalDate endDate;
}