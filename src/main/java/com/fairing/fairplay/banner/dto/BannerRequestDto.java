package com.fairing.fairplay.banner.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BannerRequestDto {

    private String title;
    private String imageUrl;
    private String linkUrl;
    private Integer priority;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String statusCode;
}
