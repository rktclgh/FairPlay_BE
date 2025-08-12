package com.fairing.fairplay.statistics.controller;

import com.fairing.fairplay.statistics.dto.reservation.ReservationRateAnalysisDto;
import com.fairing.fairplay.statistics.service.reservation.ReservationRateAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class ReservationRateAnalysisController {

    private final ReservationRateAnalysisService reservationRateAnalysisService;

    @GetMapping("/reservationRate/{eventId}")
    public ReservationRateAnalysisDto getHourlyStatisticsByDate(
            @PathVariable Long eventId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return reservationRateAnalysisService.reservationRateAnalysisDashboard(eventId, start, end);
    }
}
