package com.fairing.fairplay.temp.controller.host;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fairing.fairplay.temp.dto.host.DailyTrendDto;
import com.fairing.fairplay.temp.dto.host.HostEventReservationDto;
import com.fairing.fairplay.temp.repository.host.HostEventReservationRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/host/reservation")
@RequiredArgsConstructor
public class EventReservationController {

    private final HostEventReservationRepository hostEventReservationRepository;

    @GetMapping("/get-event-reservation-statistics/{userId}")
    public HostEventReservationDto getHostEventReservationStatistics(@PathVariable Long userId) {
        return hostEventReservationRepository.getHostEventReservationStatistics(userId);
    }

    @GetMapping("/get-daily-trend/{userId}")
    public List<DailyTrendDto> getDailyTrend(@PathVariable Long userId) {
        return hostEventReservationRepository.getDailyTrend(userId);
    }

}
