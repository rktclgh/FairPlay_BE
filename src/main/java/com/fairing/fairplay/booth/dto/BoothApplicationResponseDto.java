package com.fairing.fairplay.booth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class BoothApplicationResponseDto {
    private Long boothApplicationId;

    private String boothTitle;
    private String boothDescription;

    private String boothEmail;
    private String managerName;
    private String contactEmail;
    private String contactNumber;

    private String boothTypeName;

    private LocalDate startDate;
    private LocalDate endDate;

    private List<BoothExternalLinkDto> boothExternalLinks;

    private String boothBannerUrl;

    private String statusCode;
    private String paymentStatus;

    private LocalDateTime applyAt;
    private String adminComment;

}
