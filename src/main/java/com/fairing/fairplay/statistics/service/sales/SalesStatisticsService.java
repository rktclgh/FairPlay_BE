package com.fairing.fairplay.statistics.service.sales;

import com.fairing.fairplay.statistics.dto.sales.PaymentStatusSalesDto;
import com.fairing.fairplay.statistics.dto.sales.SalesSummaryDto;
import com.fairing.fairplay.statistics.dto.sales.SessionSalesDto;
import com.fairing.fairplay.statistics.repository.salesstats.SalesStatisticsCustomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class SalesStatisticsService {

    private final SalesStatisticsCustomRepository salesRepo;

    public Map<String, Object> getSalesDashboard(Long eventId, LocalDate start, LocalDate end) {
        // 1. summary null-safe
        SalesSummaryDto summary = salesRepo.getSalesSummary(eventId, start, end);
        if (summary == null) {
            summary = SalesSummaryDto.builder()
                    .totalSales(0L).totalCount(0)
                    .paidSales(0L).paidCount(0)
                    .cancelledSales(0L).cancelledCount(0)
                    .refundedSales(0L).refundedCount(0)
                    .build();
        }

        // 2. 리스트 null-safe
        List<PaymentStatusSalesDto> statusSales = defaultIfNull(salesRepo.getSalesByPaymentStatus(eventId, start, end));
        List<SessionSalesDto> sessionSales = defaultIfNull(salesRepo.getSessionSales(eventId, start, end));

        Map<String, Object> response = new HashMap<>();

        // ===== 상단 카드 영역 =====
        Map<String, Object> summaryMap = new HashMap<>();
        summaryMap.put("totalSales", safeLong(summary.getTotalSales()));
        summaryMap.put("totalReservations", safeInt(summary.getTotalCount()));

        Map<String, Object> paidMap = new HashMap<>();
        paidMap.put("count", safeInt(summary.getPaidCount()));
        paidMap.put("amount", safeLong(summary.getPaidSales()));

        Map<String, Object> cancelledMap = new HashMap<>();
        cancelledMap.put("count", safeInt(summary.getCancelledCount()));
        cancelledMap.put("amount", safeLong(summary.getCancelledSales()));

        Map<String, Object> refundedMap = new HashMap<>();
        refundedMap.put("count", safeInt(summary.getRefundedCount()));
        refundedMap.put("amount", safeLong(summary.getRefundedSales()));

        summaryMap.put("paid", paidMap);
        summaryMap.put("cancelled", cancelledMap);
        summaryMap.put("refunded", refundedMap);

        // ===== 결제 상태별 현황 =====
        List<Map<String, Object>> statusList = statusSales.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("label", statusLabel(s.getStatus()));
            m.put("percentage", safeDouble(s.getPercentage()));
            m.put("amount", safeLong(s.getAmount()));
            return m;
        }).toList();

        // ===== 회차별 매출 테이블 =====
        List<Map<String, Object>> sessionList = sessionSales.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("dateTime", s.getStatDate() != null ? s.getStatDate().toString() : "");
            m.put("ticketName", safeString(s.getTicketName()));
            m.put("unitPrice", safeLong(s.getUnitPrice()));
            m.put("quantity", safeInt(s.getQuantity()));
            m.put("salesAmount", safeLong(s.getSalesAmount()));
            m.put("status", statusLabel(s.getPaymentStatusCode()));
            return m;
        }).toList();

        response.put("summary", summaryMap);
        response.put("statusBreakdown", statusList);
        response.put("sessionSales", sessionList);

        return response;
    }

    // ===== null-safe Helper Methods =====
    private String statusLabel(String code) {
        if (code == null) return "기타";
        return switch (code) {
            case "COMPLETED" -> "결제 완료";
            case "CANCELLED" -> "결제 취소";
            case "REFUNDED" -> "환불 완료";
            default -> "기타";
        };
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
}
