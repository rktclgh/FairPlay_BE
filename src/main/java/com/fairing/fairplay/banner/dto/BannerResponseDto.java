package com.fairing.fairplay.banner.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BannerResponseDto {

    private Long id;
    private String title;
    private String imageUrl;
    private String linkUrl;
    private Integer priority;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String statusCode;
    private String bannerTypeCode;
    private boolean hot;
    private boolean mdPick;
    private Long eventId;
    private String smallImageUrl;

}
