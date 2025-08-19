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

    @Transactional(rollbackFor = Exception.class)
    public void runBatch(LocalDate date) {
        // 입력값 검증
        org.springframework.util.Assert.notNull(date, "date must not be null");
        AdminKpiStatistics computed = adminKpiStatsCustomRepository.calculate(date);
        // upsert 방식으로 통일 (권장)
        adminKpiStatisticsRepository.upsertAdminKpiStatistics(computed);
                log.info("관리자 KPI 통계 배치 처리 완료: {}", date);
            }
}
