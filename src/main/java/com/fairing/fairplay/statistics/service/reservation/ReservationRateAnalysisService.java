package com.fairing.fairplay.statistics.service.reservation;

import com.fairing.fairplay.statistics.dto.reservation.*;
import com.fairing.fairplay.statistics.repository.sessionstats.EventSessionStatisticsRepository;
import com.fairing.fairplay.statistics.repository.ticketstats.EventTicketStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationRateAnalysisService {

        private final EventTicketStatisticsRepository eventTicketStatisticsRepository;
        private final EventSessionStatisticsRepository eventSessionStatisticsRepository;

        public ReservationRateAnalysisDto reservationRateAnalysisDashboard(Long eventId, LocalDate start,
                        LocalDate end) {

                // 회차별 예약 통계
                List<ReservationRateBySessionDto> sessionStats = eventSessionStatisticsRepository
                                .findByEventIdAndStatDateBetween(eventId, start, end).stream()
                                .map(s -> {
                                        Integer res = s.getReservations();
                                        Integer canc = s.getCancellations();
                                        Integer stockVal = s.getStock();
                                        int reservationCount = Math.max(0,
                                                        (res == null ? 0 : res) - (canc == null ? 0 : canc));
                                        int stock = stockVal == null ? 0 : stockVal;
                                        double rate = (stock > 0) ? (reservationCount * 100.0) / stock : 0.0;

                                        return ReservationRateBySessionDto.builder()
                                                        .scheduleId(s.getSessionId())
                                                        .date(s.getStatDate())
                                                        .startTime(s.getStartTime())
                                                        .reservation(reservationCount)
                                                        .stock(stock)
                                                        .reservationRate(rate) // 계산된 예매율 세팅
                                                        .build();
                                })
                                .toList();
                // 예매율 분석 개요
                int totalStock = sessionStats.stream()
                                .mapToInt(ReservationRateBySessionDto::getStock)
                                .sum();

                int totalReservations = sessionStats.stream()
                                .mapToInt(ReservationRateBySessionDto::getReservation)
                                .sum();

                double averageReservationRate = sessionStats.stream()
                                .filter(dto -> dto.getStock() > 0)
                                .mapToDouble(dto -> (double) dto.getReservation() / dto.getStock())
                                .average()
                                .orElse(0.0) * 100;

                ReservationRateSummaryDto summary = ReservationRateSummaryDto.builder()
                                .totalTicket(totalStock)
                                .totalReservation(totalReservations)
                                .averageReservationRate(averageReservationRate)
                                .build();

                List<ReservationRateByTicketTypeDto> ticketTypeStats = eventTicketStatisticsRepository
                                .findByEventIdAndStatDateBetween(eventId, start, end).stream()
                                .map(t -> {
                                        Integer res = t.getReservations();
                                        Integer stockVal = t.getStock();
                                        int reservation = Math.max(0, res == null ? 0 : res);
                                        int stock = stockVal == null ? 0 : stockVal;
                                        double rate = (stock > 0) ? (reservation * 100.0) / stock : 0.0; // 퍼센트

                                        return ReservationRateByTicketTypeDto.builder()
                                                        .ticketType(t.getTicketType())
                                                        .reservation(reservation)
                                                        .stock(stock)
                                                        .reservationRate(rate)
                                                        .build();
                                })
                                .toList();

                return ReservationRateAnalysisDto.builder()
                                .summary(summary)
                                .sessionList(sessionStats)
                                .ticketList(ticketTypeStats)
                                .build();
        }

}
