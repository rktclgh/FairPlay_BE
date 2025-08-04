package com.fairing.fairplay.statistics.controller;

import com.fairing.fairplay.statistics.dto.EventDashboardStatsDto;
import com.fairing.fairplay.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class EventDashboardStatsController {

    private final StatisticsService statisticsService;

    @GetMapping("/reservations/{eventId}")
    public EventDashboardStatsDto getEventDashboard(
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return statisticsService.getDashboardStats(eventId, start, end);
    }
}