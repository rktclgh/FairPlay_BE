package com.fairing.fairplay.statistics.service.sales;

import com.fairing.fairplay.statistics.dto.sales.*;
import com.fairing.fairplay.statistics.repository.salesstats.SalesStatisticsCustomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesStatisticsService {

    private final SalesStatisticsCustomRepository salesRepo;

    public SalesDashboardResponse getSalesDashboard(Long eventId, LocalDate start, LocalDate end) {
        SalesSummaryDto summary = getSummaryWithDefaults(eventId, start, end);
        List<PaymentStatusSalesDto> statusSales = defaultIfNull(salesRepo.getSalesByPaymentStatus(eventId, start, end));
        List<SessionSalesDto> sessionSales = defaultIfNull(salesRepo.getSessionSales(eventId, start, end));
        List<SalesDailyTrendDto> salesDailyTrends =  defaultIfNull(salesRepo.getSalesDailyTrend(eventId, start, end));

        return SalesDashboardResponse.builder()
                .summary(buildSummarySection(summary))
                .statusBreakdown(buildStatusBreakdown(statusSales))
                .sessionSales(buildSessionSales(sessionSales))
                .salesDailyTrend(buildSalesDailyTrend(salesDailyTrends))
                .build();
    }

    private SalesDashboardResponse.SummarySection buildSummarySection(SalesSummaryDto summary) {
        return SalesDashboardResponse.SummarySection.builder()
                .totalSales(safeLong(summary.getTotalSales()))
                .totalReservations(safeInt(summary.getTotalCount()))
                .paid(buildPaymentStatusInfo(summary.getPaidCount(), summary.getPaidSales()))
                .cancelled(buildPaymentStatusInfo(summary.getCancelledCount(), summary.getCancelledSales()))
                .refunded(buildPaymentStatusInfo(summary.getRefundedCount(), summary.getRefundedSales()))
                .build();
    }

    private SalesDashboardResponse.PaymentStatusInfo buildPaymentStatusInfo(Integer count, Long amount) {
        return SalesDashboardResponse.PaymentStatusInfo.builder()
                .count(safeInt(count))
                .amount(safeLong(amount))
                .build();
    }

    private List<SalesDashboardResponse.StatusBreakdownItem> buildStatusBreakdown(List<PaymentStatusSalesDto> list) {
        return list.stream()
                .map(s -> SalesDashboardResponse.StatusBreakdownItem.builder()
                        .label(statusLabel(s.getStatus()))
                        .percentage(safeDouble(s.getPercentage()))
                        .amount(safeLong(s.getAmount()))
                        .build())
                .toList();
    }


    private List<SalesDashboardResponse.SessionSalesItem> buildSessionSales(List<SessionSalesDto> list) {
        return list.stream()
                .map(s -> SalesDashboardResponse.SessionSalesItem.builder()
                        .dateTime(s.getStatDate() != null ? s.getStatDate().toString() : "")
                        .ticketName(safeString(s.getTicketName()))
                        .unitPrice(safeLong(s.getUnitPrice()))
                        .quantity(safeInt(s.getQuantity()))
                        .salesAmount(safeLong(s.getSalesAmount()))
                        .status(statusLabel(s.getPaymentStatusCode()))
                        .build())
                .toList();
    }

    private List<SalesDashboardResponse.SalesDailyTrend> buildSalesDailyTrend(List<SalesDailyTrendDto> list) {
        return list.stream()
                .map(s -> SalesDashboardResponse.SalesDailyTrend.builder()
                        .date(s.getStatDate())
                        .amount(s.getTotalSales())
                        .count(s.getTotalCount())
                        .build())
                .toList();
    }

    private SalesSummaryDto getSummaryWithDefaults(Long eventId, LocalDate start, LocalDate end) {
        SalesSummaryDto summary = salesRepo.getSalesSummary(eventId, start, end);
        if (summary == null) {
            summary = SalesSummaryDto.builder()
                    .totalSales(0L).totalCount(0)
                    .paidSales(0L).paidCount(0)
                    .cancelledSales(0L).cancelledCount(0)
                    .refundedSales(0L).refundedCount(0)
                    .build();
        }
        return summary;
    }

    private <T> List<T> defaultIfNull(List<T> list) {
        return list != null ? list : List.of();
    }

    private String safeString(String val) {
        return val != null ? val : "";
    }

    private long safeLong(Long val) {
        return val != null ? val : 0L;
    }

    private int safeInt(Integer val) {
        return val != null ? val : 0;
    }

    private double safeDouble(Double val) {
        return val != null ? val : 0.0;
    }

    private String statusLabel(String code) {
        if (code == null) return "기타";
        return switch (code) {
            case "COMPLETED" -> "결제 완료";
            case "CANCELLED" -> "결제 취소";
            case "REFUNDED" -> "환불 완료";
            default -> "기타";
        };
    }
}
