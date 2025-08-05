package com.fairing.fairplay.statistics.controller;

import com.fairing.fairplay.statistics.dto.hourly.HourlyAnalysisResponseDto;
import com.fairing.fairplay.statistics.service.hourly.HourlyAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class HourlyStatisticsController {

    private final HourlyAnalysisService hourlyAnalysisService;

    @GetMapping("/hourly/{eventId}")
    public HourlyAnalysisResponseDto getHourlyStatistics(
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("시작 날짜는 종료 날짜보다 늦을 수 없습니다.");
        }
        return hourlyAnalysisService.analyzeHourlyBookings(eventId, start, end);
    }

    // 단일 날짜 조회용 (기존 호환성)
    @GetMapping("/hourly/{eventId}/date")
    public HourlyAnalysisResponseDto getHourlyStatisticsByDate(
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return hourlyAnalysisService.analyzeHourlyBookings(eventId, date, date);
    }
}
