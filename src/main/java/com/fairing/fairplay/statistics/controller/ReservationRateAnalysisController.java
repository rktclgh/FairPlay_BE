package com.fairing.fairplay.statistics.controller;

import com.fairing.fairplay.statistics.dto.reservation.DailyReservationRateDto;
import com.fairing.fairplay.statistics.dto.reservation.HourlyReservationRateDto;
import com.fairing.fairplay.statistics.dto.reservation.ReservationRateAnalysisDto;
import com.fairing.fairplay.statistics.service.reservation.ReservationRateAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class ReservationRateAnalysisController {

    private final ReservationRateAnalysisService reservationRateAnalysisService;

    @GetMapping("/reservationRate/{eventId}")
    public ReservationRateAnalysisDto getReservationRateAnalysis(
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작 날짜는 종료 날짜보다 늦을 수 없습니다.");
            }
        return reservationRateAnalysisService.reservationRateAnalysisDashboard(eventId, start, end);
    }

    @GetMapping("/reservationRate/{eventId}/hourly")
    public List<HourlyReservationRateDto> getHourlyReservationRate(
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작 날짜는 종료 날짜보다 늦을 수 없습니다.");
        }
        return reservationRateAnalysisService.getHourlyReservationRate(eventId, start, end);
    }

    @GetMapping("/reservationRate/{eventId}/daily")
    public List<DailyReservationRateDto> getDailyReservationRate(
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작 날짜는 종료 날짜보다 늦을 수 없습니다.");
        }
        return reservationRateAnalysisService.getDailyReservationRate(eventId, start, end);
    }
}
