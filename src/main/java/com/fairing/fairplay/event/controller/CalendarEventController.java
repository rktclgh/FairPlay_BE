package com.fairing.fairplay.event.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.dto.CalendarEventDto;
import com.fairing.fairplay.event.dto.CalendarGroupedDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.fairing.fairplay.event.service.CalendarEventService;
import java.util.List;
import org.springframework.ui.Model;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.service.EventService;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
public class CalendarEventController {

    private final CalendarEventService calendarEventService;
    private final EventService eventService;

    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventDto>> getEventsByMonth(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam int year,
            @RequestParam int month
    ) {
        List<CalendarEventDto> events = calendarEventService.getMonthlyEventsForUser(user.getUserId(), year, month);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/events/grouped")
    public ResponseEntity<List<CalendarGroupedDto>> getGroupedEvents(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        List<CalendarGroupedDto> grouped = calendarEventService.getGroupedEventsByUser(user.getUserId());
        return ResponseEntity.ok(grouped);
    }
}

