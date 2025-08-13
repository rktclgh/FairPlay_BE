package com.fairing.fairplay.statistics.controller;


import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.statistics.dto.kpi.*;
import com.fairing.fairplay.statistics.service.kpi.AdminKpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminKpiController {

    private final AdminKpiService adminKpiService;

    @GetMapping("/summary")
    @FunctionAuth("getAdminKpiSummary")
    public KpiSummaryDto getAdminKpiSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return adminKpiService.summaryKpi(startDate, endDate);
    }

    @GetMapping("/trend-month")
    @FunctionAuth("getMonthlyTrend")
    public List<KpiTrendMonthlyDto> getMonthlyTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate

    ) {
        return  adminKpiService.monthlyTrend(startDate, endDate);
    }

    @GetMapping("/trend-daily")
    @FunctionAuth("getDailyTrend")
    public List<KpiTrendDto> getDailyTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate

    ) {
        return adminKpiService.dailyTrend(startDate, endDate);
    }
}
