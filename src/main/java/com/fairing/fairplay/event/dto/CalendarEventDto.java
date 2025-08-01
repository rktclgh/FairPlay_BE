package com.fairing.fairplay.event.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class CalendarEventDto {
    private Long eventId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String regionName;
}
