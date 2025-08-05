package com.fairing.fairplay.statistics.controller;

import com.fairing.fairplay.statistics.service.sales.SalesStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class SalesStatisticsController {

    private final SalesStatisticsService salesService;

    @GetMapping("/sales/{eventId}")
    public Map<String, Object> getSalesStatistics(
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return salesService.getSalesDashboard(eventId, start, end);
    }
}

