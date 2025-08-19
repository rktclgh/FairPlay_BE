package com.fairing.fairplay.statistics.service.reservation;

import com.fairing.fairplay.statistics.dto.reservation.*;
import com.fairing.fairplay.statistics.entity.reservation.EventDailyStatistics;
import com.fairing.fairplay.statistics.repository.dailystats.DailyStatsCustomRepository;
import com.fairing.fairplay.statistics.repository.dailystats.EventDailyStatisticsRepository;
import com.fairing.fairplay.statistics.repository.hourlystats.EventHourlyStatisticsRepository;
import com.fairing.fairplay.statistics.repository.sessionstats.EventSessionStatisticsRepository;
import com.fairing.fairplay.statistics.repository.ticketstats.EventTicketStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

        private final EventDailyStatisticsRepository dailyRepo;
        private final EventHourlyStatisticsRepository hourlyRepo;
        private final EventTicketStatisticsRepository ticketRepo;
        private final EventSessionStatisticsRepository sessionRepo;
        private final DailyStatsCustomRepository dailyStatsCustomRepository;

        // 데이터 집계
        @Transactional
        public void runBatch(LocalDate date) {
                try {
                        dailyRepo.saveAll(dailyStatsCustomRepository.calculate(date));
                        hourlyRepo.saveAll(hourlyRepo.calculate(date));
                        ticketRepo.saveAll(ticketRepo.calculate(date));
                        sessionRepo.saveAll(sessionRepo.calculate(date));
                        log.info("통계 배치 처리 완료: {}", date);
                } catch (Exception e) {
                        log.error("통계 배치 처리 실패: {}", date, e);
                        throw e;
                }
        }

        public EventDashboardStatsDto getDashboardStats(Long eventId, LocalDate start, LocalDate end) {
                // 날짜 범위 검증
                if (start.isAfter(end)) {
                        throw new IllegalArgumentException("시작일이 종료일보다 늦을 수 없습니다.");
                }
                // 1. 요약 데이터
                List<EventDailyStatistics> dailyList = dailyRepo.findByEventIdAndStatDateBetweenOrderByStatDate(eventId,
                                start, end);
                int totalReservations = dailyList.stream().mapToInt(EventDailyStatistics::getReservationCount).sum();
                int totalCheckins = dailyList.stream().mapToInt(EventDailyStatistics::getCheckinsCount).sum();
                int totalCancellations = dailyList.stream().mapToInt(EventDailyStatistics::getCancellationCount).sum();
                int totalNoShows = dailyList.stream().mapToInt(EventDailyStatistics::getNoShowsCount).sum();

                ReservationSummaryDto summary = ReservationSummaryDto.builder()
                                .totalReservations(totalReservations)
                                .totalCheckins(totalCheckins)
                                .totalCancellations(totalCancellations)
                                .totalNoShows(totalNoShows)
                                .build();

                // 2. 날짜별 예약 추이
                List<ReservationDailyTrendDto> dailyTrend = dailyList.stream()
                                .map(s -> ReservationDailyTrendDto.builder()
                                                .date(s.getStatDate())
                                                .reservations(s.getReservationCount())
                                                .build())
                                .toList();

                // 3. 티켓별 예약 비율
                List<TicketRatioDto> ticketRatio = ticketRepo.findByEventIdAndStatDateBetween(eventId, start, end)
                                .stream()
                                .map(t -> TicketRatioDto.builder()
                                                .ticketType(t.getTicketType())
                                                .reservations(t.getReservations())
                                                .build())
                                .toList();

                // 4. 회차별 예약 통계
                List<SessionStatsDto> sessionStats = sessionRepo.findByEventIdAndStatDateBetween(eventId, start, end)
                                .stream()
                                .map(s -> SessionStatsDto.builder()
                                                .sessionId(s.getSessionId())
                                                .statDate(s.getStatDate())
                                                .startTime(s.getStartTime())
                                                .sessionName(s.getTicketType()) // 필요시 이름 매핑
                                                .reservations(s.getReservations())
                                                .checkins(s.getCheckins())
                                                .cancellations(s.getCancellations())
                                                .noShows(s.getNoShows())
                                                .build())
                                .toList();

                return EventDashboardStatsDto.builder()
                                .summary(summary)
                                .dailyTrend(dailyTrend)
                                .ticketRatio(ticketRatio)
                                .sessionStats(sessionStats)
                                .build();
        }
}