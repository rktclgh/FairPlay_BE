package com.fairing.fairplay.statistics.service.sales;


import com.fairing.fairplay.statistics.repository.salesstats.AdminSalesStatisticsRepository;
import com.fairing.fairplay.statistics.repository.salesstats.AdminSalesStatsCustomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSalesStatisticsBatchService {

    private final AdminSalesStatsCustomRepository adminSalesStatsCustomRepository;
    private final AdminSalesStatisticsRepository adminSalesStatisticsRepository;

    @Transactional
    public void runBatch(LocalDate date) {
        try {
            adminSalesStatisticsRepository.save(adminSalesStatsCustomRepository.calculate(date));
            log.info("관리자 매출 통계 배치 처리 완료: {}", date);
        } catch (Exception e) {
            log.error("관리자 매출 통계 배치 처리 실패: {}", date, e);
            throw e;
        }
    }
}
