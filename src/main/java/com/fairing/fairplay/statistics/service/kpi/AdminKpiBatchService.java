package com.fairing.fairplay.statistics.service.kpi;


import com.fairing.fairplay.statistics.entity.kpi.AdminKpiStatistics;
import com.fairing.fairplay.statistics.repository.kpistats.AdminKpiStatisticsRepository;
import com.fairing.fairplay.statistics.repository.kpistats.AdminKpiStatsCustomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminKpiBatchService {

    private final AdminKpiStatsCustomRepository adminKpiStatsCustomRepository;
    private final AdminKpiStatisticsRepository adminKpiStatisticsRepository;

    @Transactional
    public void runBatch(LocalDate date) {
        try {
            AdminKpiStatistics computed = adminKpiStatsCustomRepository.calculate(date);
            adminKpiStatisticsRepository.findByStatDate(date)
                    .ifPresent(existing -> {
                log.warn("기존 KPI 데이터 발견, 재계산합니다: {}", date);
                adminKpiStatisticsRepository.delete((AdminKpiStatistics) existing);
                });
            adminKpiStatisticsRepository.save(computed);
            log.info("관리자 KPI 통계 배치 처리 완료: {}", date);
        } catch (Exception e) {
            log.error("관리자 KPI 통계 배치 처리 실패: {}", date, e);
            throw e;
        }
    }
}
