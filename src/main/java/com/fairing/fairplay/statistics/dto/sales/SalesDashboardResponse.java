package com.fairing.fairplay.statistics.dto.sales;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class SalesDashboardResponse {
    private SummarySection summary;
    private List<StatusBreakdownItem> statusBreakdown;
    private List<SessionSalesItem> sessionSales;
    private List<SalesDailyTrend> salesDailyTrend;

    @Getter
    @Builder
    public static class SummarySection {
        private Long totalSales;
        private Integer totalReservations;
        private PaymentStatusInfo paid;
        private PaymentStatusInfo cancelled;
        private PaymentStatusInfo refunded;
    }

    @Getter
    @Builder
    public static class PaymentStatusInfo {
        private Integer count;
        private Long amount;
    }

    @Getter
    @Builder
    public static class StatusBreakdownItem {
        private String label;
        private Double percentage;
        private Long amount;
    }

    @Getter
    @Builder
    public static class SessionSalesItem {
        private String dateTime;
        private String ticketName;
        private Long unitPrice;
        private Integer quantity;
        private Long salesAmount;
        private String status;
    }

    @Getter
    @Builder
    public static class SalesDailyTrend {
        private LocalDate date;
        private Long amount;
        private Integer count;

    }
}
