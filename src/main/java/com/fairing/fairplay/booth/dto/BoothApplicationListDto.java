package com.fairing.fairplay.booth.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class BoothApplicationListDto {
    private Long boothApplicationId;
    private String boothTitle;
    private LocalDateTime applyAt;

    private String statusCode;
    private String statusName;
    private String paymentStatus;
    private String paymentStatusCode;
    private String boothTypeName;
    private String boothTypeSize;
    private String eventTitle;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer price;
    private String managerName;
    private String contactEmail;

    private Long boothTypeId;
}
