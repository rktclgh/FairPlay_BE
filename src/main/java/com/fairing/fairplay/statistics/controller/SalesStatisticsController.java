package com.fairing.fairplay.statistics.controller;

import com.fairing.fairplay.statistics.dto.sales.SalesDashboardResponse;
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
    public SalesDashboardResponse getSalesStatistics(
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("시작 날짜는 종료 날짜보다 늦을 수 없습니다.");
        }
        return salesService.getSalesDashboard(eventId, start, end);
    }
}

