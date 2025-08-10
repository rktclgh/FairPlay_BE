package com.fairing.fairplay.statistics.service.event;

import com.fairing.fairplay.statistics.repository.eventstats.EventComparisonStatisticsRepository;
import com.fairing.fairplay.statistics.repository.eventstats.EventPopularityStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;


@Service
@RequiredArgsConstructor
@Slf4j
public class EventBatchService {

    private final EventPopularityStatisticsRepository PopRepo;
    private final EventComparisonStatisticsRepository ComparisonRepo;

    // 데이터 집계
    @Transactional
    public void runBatch(LocalDate date) {
        try {
            ComparisonRepo.saveAll(ComparisonRepo.calculate(date));
            PopRepo.saveAll(PopRepo.calculate(date));
            log.info("행사 및 인기행사 통계 배치 처리 완료: {}", date);
        } catch (Exception e) {
            log.error("행사 및 인기행사 통계 배치 처리 실패: {}", date, e);
            throw e;
        }
    }
}
