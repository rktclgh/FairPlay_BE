package com.fairing.fairplay.booth.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoothApplicationListDto {
    private Long id;
    private String boothTitle;
    private LocalDateTime applyAt;

    private String statusCode;
    private String paymentStatus;
}
