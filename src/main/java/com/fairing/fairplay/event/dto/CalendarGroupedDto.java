package com.fairing.fairplay.event.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
    @AllArgsConstructor
    public class CalendarGroupedDto {
        private LocalDate date;
        private List<String> titles;

}
