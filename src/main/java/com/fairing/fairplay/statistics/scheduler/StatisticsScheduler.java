package com.fairing.fairplay.statistics.scheduler;

import com.fairing.fairplay.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class StatisticsScheduler {

    private final StatisticsService batchService;

    @Scheduled(cron = "0 5 0 * * *") // 매일 00:05
    public void runDailyBatch() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        batchService.runBatch(yesterday);
    }
}