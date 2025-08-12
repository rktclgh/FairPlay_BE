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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
public class CalendarEventController {

    private final CalendarEventService calendarEventService;

    // 리스트형 (월별 이벤트 목록)
    @GetMapping("/events")
    public ResponseEntity<List<CalendarEventDto>> getEventsByMonth(
            @AuthenticationPrincipal CustomUserDetails user, // nullable 허용
            @RequestParam int year,
            @RequestParam int month
    ) {
        // 현재는 userId 미사용 → 필요해지면 필터링 로직 추가
        List<CalendarEventDto> events = calendarEventService.getMonthlyEvents(year, month);
        return ResponseEntity.ok(events);
    }

    // 캘린더형 (날짜별 그룹)
    @GetMapping("/events/grouped")
    public ResponseEntity<List<CalendarGroupedDto>> getGroupedEvents(
            @AuthenticationPrincipal CustomUserDetails user, // nullable 허용
            @RequestParam int year,
            @RequestParam int month
    ) {
        List<CalendarGroupedDto> grouped = calendarEventService.getGroupedEventsByDate(year, month);
        return ResponseEntity.ok(grouped);
    }
}

