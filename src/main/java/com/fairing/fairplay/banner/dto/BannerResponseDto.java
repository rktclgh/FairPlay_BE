package com.fairing.fairplay.banner.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BannerResponseDto {

    private Integer id;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private Integer priority;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String statusCode;
}
