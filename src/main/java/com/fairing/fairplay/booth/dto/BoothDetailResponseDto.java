package com.fairing.fairplay.booth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class BoothDetailResponseDto {

    private Long boothId;
    private String boothTitle;
    private String boothBannerUrl;
    private String boothDescription;
    private String boothTypeName;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String managerName;
    private String contactEmail;
    private String contactNumber;

    private List<BoothExternalLinkDto> boothExternalLinks;
}
