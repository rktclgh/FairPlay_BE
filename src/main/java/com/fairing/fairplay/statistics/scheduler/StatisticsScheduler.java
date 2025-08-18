package com.fairing.fairplay.statistics.scheduler;

import com.fairing.fairplay.statistics.service.event.EventBatchService;
import com.fairing.fairplay.statistics.service.kpi.AdminKpiBatchService;
import com.fairing.fairplay.statistics.service.reservation.StatisticsService;
import com.fairing.fairplay.statistics.service.sales.AdminSalesStatisticsBatchService;
import com.fairing.fairplay.statistics.service.sales.SalesStatisticsBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;


@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsScheduler {

    private final StatisticsService statisticsService;
    private final SalesStatisticsBatchService salesBatchService;
    private final EventBatchService eventbatchService;
    private final AdminKpiBatchService adminKpiBatchService;
    private final AdminSalesStatisticsBatchService adminSalesStatisticsBatchService;

    @Scheduled(cron = "0 5 0 * * *") // 매일 00:05
    public void runDailyBatch() {
        try {
            LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
            log.info("Starting daily statistics batch for date: {}", yesterday);
            statisticsService.runBatch(yesterday);
            log.info("Completed daily statistics batch for date: {}", yesterday);
        } catch (Exception e) {
            log.error("Failed to run daily statistics batch", e);
        }
    }
    @Scheduled(cron = "0 15 0 * * *")
    public void dailyAggregation() {
        try {
            LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
            log.info("Starting daily sales statistics batch for date: {}", yesterday);
            salesBatchService.aggregateDailySales(yesterday);
            log.info("Completed daily sales  statistics batch for date: {}", yesterday);
        } catch (Exception e) {
            log.error("Failed to run daily  sales statistics batch", e);
        }
    }

    @Scheduled(cron = "0 25 0 * * *")
    public void eventAggregation() {
        try {
            LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
            log.info("Starting daily events statistics batch for date: {}", yesterday);
            eventbatchService.runBatch(yesterday);
            log.info("Completed daily events  statistics batch for date: {}", yesterday);
        } catch (Exception e) {
            log.error("Failed to run daily events statistics batch", e);
        }
    }

    @Scheduled(cron = "0 35 0 * * *")
    public void adminKpiAggregation() {
        try {
            LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
            log.info("Starting Admin Kpi statistics batch for date: {}", yesterday);
            adminKpiBatchService.runBatch(yesterday);
            log.info("Completed Admin Kpi  statistics batch for date: {}", yesterday);
        } catch (Exception e) {
            log.error("Failed to run Admin Kpi statistics batch", e);
        }
    }

    @Scheduled(cron = "0 45 0 * * *")
    public void adminSalesAggregation() {
        try {
            LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
            log.info("Starting Admin sales statistics batch for date: {}", yesterday);
            adminSalesStatisticsBatchService.runBatch(yesterday);
            log.info("Completed Admin sales  statistics batch for date: {}", yesterday);
        } catch (Exception e) {
            log.error("Failed to run Admin sales statistics batch", e);
        }
    }

}