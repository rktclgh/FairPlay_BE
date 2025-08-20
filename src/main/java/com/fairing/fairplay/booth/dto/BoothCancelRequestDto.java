package com.fairing.fairplay.booth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoothCancelRequestDto {
    private String cancelReason;
    private String contactEmail;
}