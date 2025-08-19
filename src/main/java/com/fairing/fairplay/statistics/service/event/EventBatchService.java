package com.fairing.fairplay.statistics.service.event;

import com.fairing.fairplay.statistics.entity.event.EventComparisonStatistics;
import com.fairing.fairplay.statistics.entity.event.EventPopularityStatistics;
import com.fairing.fairplay.statistics.repository.eventstats.EventComparisonStatisticsRepository;
import com.fairing.fairplay.statistics.repository.eventstats.EventPopularityStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class EventBatchService {

    private final EventPopularityStatisticsRepository eventPopularityStatisticsRepository;
    private final EventComparisonStatisticsRepository eventComparisonStatisticsRepository;

    // 데이터 집계
    @Transactional(rollbackFor = Exception.class)
    public void runBatch(LocalDate date) {
        try {
            // Event Comparison Statistics 처리
            List<EventComparisonStatistics> comparisonStats = eventComparisonStatisticsRepository.calculate(date);
            for (EventComparisonStatistics stats : comparisonStats) {
                eventComparisonStatisticsRepository.upsertEventComparisonStatistics(stats);
            }
            log.info("Event Comparison Statistics 배치 처리 완료: {} 건", comparisonStats.size());

            // Event Popularity Statistics 처리
            List<EventPopularityStatistics> popularityStats = eventPopularityStatisticsRepository.calculate(date);
            for (EventPopularityStatistics stats : popularityStats) {
                eventPopularityStatisticsRepository.upsertEventPopularityStatistics(stats);
            }
            log.info("Event Popularity Statistics 배치 처리 완료: {} 건", popularityStats.size());

            log.info("행사 및 인기행사 통계 배치 처리 완료: {}", date);
        } catch (Exception e) {
            log.error("행사 및 인기행사 통계 배치 처리 실패: {}", date, e);
            throw e;
        }
    }
}
