package com.fairing.fairplay.statistics.service.sales;

import com.fairing.fairplay.statistics.dto.sales.RawSalesData;
import com.fairing.fairplay.statistics.entity.sales.EventDailySalesStatistics;
import com.fairing.fairplay.statistics.entity.sales.EventSessionSalesStatistics;
import com.fairing.fairplay.statistics.repository.salesstats.EventDailySalesStatisticsRepository;
import com.fairing.fairplay.statistics.repository.salesstats.EventSessionSalesStatisticsRepository;
import com.fairing.fairplay.statistics.repository.salesstats.SalesRawDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesStatisticsBatchService {

    private final SalesRawDataRepository rawRepo; // payment, reservation 등 조인해서 가져오는 레포
    private final EventDailySalesStatisticsRepository dailyRepo;
    private final EventSessionSalesStatisticsRepository sessionRepo;

    @Transactional
    public void aggregateDailySales(LocalDate targetDate) {
        log.info("매출 집계 시작: {}", targetDate);

        // 원본 데이터 조회 (결제/환불 등)
        List<RawSalesData> rawDataList = rawRepo.fetchSalesData(targetDate);

        Map<Long, EventDailySalesStatistics> dailyStatsMap = new HashMap<>();
        List<EventSessionSalesStatistics> sessionStatsList = new ArrayList<>();

        for (RawSalesData raw : rawDataList) {
            // === 일별 집계 ===
            EventDailySalesStatistics daily = dailyStatsMap.computeIfAbsent(
                    raw.getEventId(),
                    k -> EventDailySalesStatistics.builder()
                            .eventId(raw.getEventId())
                            .statDate(targetDate)
                            .totalSales(0L)
                            .totalCount(0)
                            .paidSales(0L)
                            .paidCount(0)
                            .cancelledSales(0L)
                            .cancelledCount(0)
                            .refundedSales(0L)
                            .refundedCount(0)
                            .build()
            );

            daily.setTotalSales(daily.getTotalSales() + raw.getAmount());
            daily.setTotalCount(daily.getTotalCount() + raw.getQuantity());

            switch (raw.getPaymentStatus()) {
                case "COMPLETED" -> {
                    daily.setPaidSales(daily.getPaidSales() + raw.getAmount());
                    daily.setPaidCount(daily.getPaidCount() + raw.getQuantity());
                }
                case "CANCELLED" -> {
                    daily.setCancelledSales(daily.getCancelledSales() + raw.getAmount());
                    daily.setCancelledCount(daily.getCancelledCount() + raw.getQuantity());
                }
                case "REFUNDED" -> {
                    daily.setRefundedSales(daily.getRefundedSales() + raw.getAmount());
                    daily.setRefundedCount(daily.getRefundedCount() + raw.getQuantity());
                }
            }

            // === 회차별 집계 ===
            sessionStatsList.add(EventSessionSalesStatistics.builder()
                    .eventId(raw.getEventId())
                    .scheduleId(raw.getScheduleId())
                    .statDate(targetDate)
                    .ticketName(raw.getTicketName())
                    .unitPrice(raw.getUnitPrice())
                    .quantity(raw.getQuantity())
                    .salesAmount(raw.getAmount())
                    .paymentStatusCode(raw.getPaymentStatus())
                    .build());
        }

        dailyRepo.saveAll(dailyStatsMap.values());
        sessionRepo.saveAll(sessionStatsList);

        log.info("매출 집계 완료: {}", targetDate);
    }
}
