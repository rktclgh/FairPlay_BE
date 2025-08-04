package com.fairing.fairplay.booth.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class BoothApplicationRequestDto {
    private Long eventId;
    private Long boothTypeId;

    private String boothTitle;
    private String boothDescription;

    private String managerName;
    private String email;
    private String contactNumber;
    private String officialUrl;

    private LocalDate startDate;
    private LocalDate endDate;

}
