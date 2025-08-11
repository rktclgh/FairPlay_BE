package com.fairing.fairplay.statistics.service.event;

import com.fairing.fairplay.statistics.entity.event.EventComparisonStatistics;
import com.fairing.fairplay.statistics.repository.eventstats.EventComparisonStatisticsRepository;
import com.fairing.fairplay.statistics.dto.event.EventComparisonPageResponseDto;
import com.fairing.fairplay.statistics.dto.event.EventComparisonResponseDto;
import com.fairing.fairplay.statistics.dto.event.EventStatsOverviewResponseDto;
import com.fairing.fairplay.statistics.repository.eventstats.EventComparisonStatsCustomRepositoryImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventComparisonService {



    private final EventComparisonStatisticsRepository comparisonRepository;
    private final EventComparisonStatsCustomRepositoryImpl customRepository;

    /**
     * 행사별 비교 페이지의 모든 데이터를 한 번에 반환
     */
    public EventComparisonPageResponseDto getComparisonPageData() {
        // 1. 전체 행사 통계 조회
        List<EventComparisonStatistics> allStats = comparisonRepository.findAll();
        List<EventComparisonResponseDto> allEvents = allStats.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // 2. 전체 통계 계산
        EventStatsOverviewResponseDto overallStats = calculateOverallStats(allStats);

        // 3. 매출 상위 행사 TOP 3
        List<EventComparisonResponseDto> topRevenueEvents = allEvents.stream()
                .sorted(java.util.Comparator
                .comparing(EventComparisonResponseDto::getTotalSales,
                java.util.Comparator.nullsLast(Long::compareTo))
                .reversed())
        .limit(3)
                .collect(Collectors.toList());

        // 4. 상태별 통계
        Map<String, Long> statusCounts = allEvents.stream()
                .collect(Collectors.groupingBy(
                        EventComparisonResponseDto::getStatus,
                        Collectors.counting()
                ));

        // 5. 차트 데이터 생성
        /*List<Map<String, Object>> chartData = allEvents.stream()
                .limit(10) // 상위 10개 행사
                .map(event -> Map.of(
                        "name", truncateEventName(event.getEventName()),
                        "매출", event.getTotalSales() / 1000000, // 백만원 단위
                        "예약수", event.getTotalReservations()
                ))
                .collect(Collectors.toList());*/

        return EventComparisonPageResponseDto.builder()
                .allEvents(allEvents)
                .overallStats(overallStats)
                .topRevenueEvents(topRevenueEvents)
                .statusCounts(statusCounts)
                //.chartData(chartData)
                .build();
    }

    /**
     * 상태별 필터링된 행사 목록 반환
     */
    public List<EventComparisonResponseDto> getEventsByStatus(String status) {
        List<EventComparisonStatistics> stats;

        String normalized = (status == null) ? "all" : status.trim().toLowerCase(java.util.Locale.ROOT);
        if ("all".equals(normalized))  {
            stats = comparisonRepository.findAll();
        } else {
            stats = customRepository.findByStatus(normalized, LocalDate.now());
        }

        return stats.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }


    private EventComparisonResponseDto convertToResponse(EventComparisonStatistics stats) {
        String status = determineEventStatus(stats.getStartDate(), stats.getEndDate());

        return EventComparisonResponseDto.builder()
                .statsId(stats.getStatsComparisonId())
                .eventId(stats.getEventId())
                .eventName(stats.getEventTitle()) // 실제로는 Event 엔티티 조인 필요
                .status(status)
                .totalUsers(stats.getTotalUsers())
                .totalReservations(stats.getTotalReservations())
                .totalSales(stats.getTotalSales())
                .avgTicketPrice(stats.getAvgTicketPrice())
                .cancellationRate(stats.getCancellationRate())
                .startDate(stats.getStartDate())
                .endDate(stats.getEndDate())
                .lastUpdatedAt(stats.getLastUpdatedAt())
                .build();
    }

    private EventStatsOverviewResponseDto calculateOverallStats(List<EventComparisonStatistics> allStats) {
        long totalUsers = allStats.stream()
                .mapToLong(s -> s.getTotalUsers() != null ? s.getTotalUsers() : 0)
                .sum();

        long totalReservations = allStats.stream()
                .mapToLong(s -> s.getTotalReservations() != null ? s.getTotalReservations() : 0)
                .sum();

        long totalSales = allStats.stream()
                .mapToLong(s -> s.getTotalSales() != null ? s.getTotalSales() : 0)
                .sum();

        return EventStatsOverviewResponseDto.builder()
                .totalUsers(totalUsers)
                .totalReservations(totalReservations)
                .totalSales(BigDecimal.valueOf(totalSales))
                .totalEvents((long) allStats.size())
                .build();
    }

    private String determineEventStatus(LocalDate startDate, LocalDate endDate) {
        LocalDate now = LocalDate.now();
        if (startDate != null && now.isBefore(startDate)) {
            return "upcoming";
        } else if (endDate != null && now.isAfter(endDate)) {
            return "ended";
        } else {
            return "ongoing";
        }
    }

    private String truncateEventName(String eventName) {
        if (eventName == null) return "";
        return eventName.length() > 10 ? eventName.substring(0, 10) + "..." : eventName;
    }
}