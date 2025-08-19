package com.fairing.fairplay.booth.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BoothCancelPageDto {
    private Long applicationId;
    private String eventTitle;
    private String boothTitle;
    private String boothTypeName;
    private String boothTypeSize;
    private Integer price;
    private String managerName;
    private String contactEmail;
    private String applicationStatus;
    private String paymentStatus;
    private boolean canCancel;
    private String cancelReason;
}