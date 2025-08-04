package com.fairing.fairplay.statistics.scheduler;

import com.fairing.fairplay.statistics.service.StatisticsService;
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
}