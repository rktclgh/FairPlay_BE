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
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam int year,
            @RequestParam int month
    ) {
        Long userId = userDetails.getUserId();

        List<CalendarEventDto> events = calendarEventService.getMonthlyEventsForUser(userId, year, month);

        return ResponseEntity.ok(events);
    }


/*
    @GetMapping("/reservation/{eventId}")
    public String showReservationForm(@PathVariable Long eventId, Model model) {
        Event event = eventService.findById(eventId);
        model.addAttribute("event", event);
        return "reservation-form"; // reservation-form.jsp or .html
    }*/


    @GetMapping("/events/grouped")
    public ResponseEntity<List<CalendarGroupedDto>> getGroupedEvents(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        List<CalendarGroupedDto> grouped = calendarEventService.getGroupedEventsByUser(userId);
        return ResponseEntity.ok(grouped);
    }


}
