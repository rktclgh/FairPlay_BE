package com.fairing.fairplay.banner.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;

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

    private String s3Key;
    private String originalFileName;
    private String fileType;
    private Long fileSize;

    @NotNull(message = "eventId는 필수입니다.")
    private Long eventId;

    @NotNull
    private Long bannerTypeId;
    private boolean hot;
    private boolean mdPick;

}
