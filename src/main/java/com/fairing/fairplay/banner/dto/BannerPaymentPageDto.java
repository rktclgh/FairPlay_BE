package com.fairing.fairplay.banner.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class BannerPaymentPageDto {
    private Long applicationId;
    private String title;
    private String bannerType;
    private Integer totalAmount;
    private String applicantName;
    private String applicantEmail;
    private String paymentStatus;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}