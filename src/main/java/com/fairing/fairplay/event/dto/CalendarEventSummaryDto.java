package com.fairing.fairplay.event.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@lombok.EqualsAndHashCode(of = {"eventId","title"})
public class CalendarEventSummaryDto {
    @lombok.NonNull private Long eventId;
    @lombok.NonNull private String title;
}
