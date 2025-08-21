package com.fairing.fairplay.statistics.controller;

import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.statistics.dto.hourly.DayOfWeekSummaryDto;
import com.fairing.fairplay.statistics.dto.hourly.HourlyAnalysisResponseDto;
import com.fairing.fairplay.statistics.dto.hourly.MonthlyTimePeriodDto;
import com.fairing.fairplay.statistics.service.hourly.HourlyAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class HourlyStatisticsController {

    private final HourlyAnalysisService hourlyAnalysisService;

    @GetMapping("/hourly/{eventId}")
    @FunctionAuth("getHourlyStatistics")
    public HourlyAnalysisResponseDto getHourlyStatistics(
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작 날짜는 종료 날짜보다 늦을 수 없습니다.");
        }
        return hourlyAnalysisService.analyzeHourlyBookings(eventId, start, end);
    }

    // 단일 날짜 조회용 (기존 호환성)
    @GetMapping("/hourly/{eventId}/date")
    @FunctionAuth("getHourlyStatisticsByDate")
    public HourlyAnalysisResponseDto getHourlyStatisticsByDate(
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return hourlyAnalysisService.analyzeHourlyBookings(eventId, date, date);
    }

    @GetMapping("/hourly/{eventId}/day-of-week")
    @FunctionAuth("getHourlyStatisticsByDayOfWeek")
    public HourlyAnalysisResponseDto getHourlyStatisticsByDayOfWeek(
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작 날짜는 종료 날짜보다 늦을 수 없습니다.");
        }
        return hourlyAnalysisService.analyzeHourlyBookingsByDayOfWeek(eventId, start, end);
    }

    @GetMapping("/daily/{eventId}/day-of-week")
    @FunctionAuth("getDayOfWeekSummary")
    public List<DayOfWeekSummaryDto> getDayOfWeekSummary(
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작 날짜는 종료 날짜보다 늦을 수 없습니다.");
        }
        return hourlyAnalysisService.getDayOfWeekSummary(eventId, start, end);
    }

    @GetMapping("/monthly/{eventId}/time-period")
    @FunctionAuth("getMonthlyTimePeriodSummary")
    public List<MonthlyTimePeriodDto> getMonthlyTimePeriodSummary(
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작 날짜는 종료 날짜보다 늦을 수 없습니다.");
        }
        return hourlyAnalysisService.getMonthlyTimePeriodSummary(eventId, start, end);
    }
}
