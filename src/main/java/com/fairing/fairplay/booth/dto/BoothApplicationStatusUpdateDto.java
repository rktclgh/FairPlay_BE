package com.fairing.fairplay.booth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoothApplicationStatusUpdateDto {
    private String statusCode; // APPROVED or REJECTED
    private String adminComment;
}
