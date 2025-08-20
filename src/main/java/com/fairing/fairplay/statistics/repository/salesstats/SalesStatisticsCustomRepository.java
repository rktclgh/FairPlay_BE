package com.fairing.fairplay.statistics.repository.salesstats;

import com.fairing.fairplay.statistics.dto.sales.PaymentStatusSalesDto;
import com.fairing.fairplay.statistics.dto.sales.SalesDailyTrendDto;
import com.fairing.fairplay.statistics.dto.sales.SalesSummaryDto;
import com.fairing.fairplay.statistics.dto.sales.SessionSalesDto;
import java.time.LocalDate;
import java.util.List;

public interface SalesStatisticsCustomRepository {
    SalesSummaryDto getSalesSummary(Long eventId, LocalDate start, LocalDate end);

    List<SalesDailyTrendDto> getSalesDailyTrend(Long eventId, LocalDate start, LocalDate end);

    List<PaymentStatusSalesDto> getSalesByPaymentStatus(Long eventId, LocalDate start, LocalDate end);
    List<SessionSalesDto> getSessionSales(Long eventId, LocalDate start, LocalDate end);
}