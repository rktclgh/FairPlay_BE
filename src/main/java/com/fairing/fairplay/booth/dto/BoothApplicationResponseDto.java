package com.fairing.fairplay.booth.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class BoothApplicationResponseDto {
    private Long id;

    private String eventTitle;
    private String boothTitle;
    private String boothDescription;

    private String managerName;
    private String boothEmail;
    private String contactEmail;
    private String contactNumber;
    private String officialUrl;

    private String boothTypeName;

    private LocalDate startDate;
    private LocalDate endDate;

    private String statusCode;
    private String paymentStatus;

    private LocalDateTime applyAt;
    private String adminComment;
}
