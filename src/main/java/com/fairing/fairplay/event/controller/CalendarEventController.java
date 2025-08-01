package com.fairing.fairplay.event.controller;

import com.fairing.fairplay.event.dto.CalendarEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.fairing.fairplay.event.service.CalendarEventService;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
public class CalendarEventController {

    private final CalendarEventService calendarEventService;

    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventDto>> getEventsByMonth(
            @RequestParam int year,
            @RequestParam int month
    ) {
        List<CalendarEventDto> events = calendarEventService.getMonthlyEvents(year, month);
        return ResponseEntity.ok(events);
    }
}
