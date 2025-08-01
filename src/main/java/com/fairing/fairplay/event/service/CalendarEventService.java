package com.fairing.fairplay.event.service;

import com.fairing.fairplay.event.dto.CalendarEventDto;
import com.fairing.fairplay.event.repository.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarEventService {

    private final CalendarEventRepository calendarEventRepository;

    public List<CalendarEventDto> getMonthlyEvents(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        return calendarEventRepository.findEventsByMonth(start, end);
    }
}

