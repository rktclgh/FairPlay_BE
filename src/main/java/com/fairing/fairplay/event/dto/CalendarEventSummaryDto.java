package com.fairing.fairplay.event.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CalendarEventSummaryDto {
    private Long eventId;
    private String title;
}
