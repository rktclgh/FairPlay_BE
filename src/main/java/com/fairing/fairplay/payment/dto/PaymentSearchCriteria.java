package com.fairing.fairplay.payment.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSearchCriteria {

    // 결제 항목 (다중 선택 가능)
    private List<String> paymentTypes; // RESERVATION, BOOTH, AD

    // 결제일 범위
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    // 결제 상태 (다중 선택 가능)
    private List<String> paymentStatuses; // PENDING, COMPLETED, CANCELLED, REFUNDED

    // 행사명 검색 (부분 검색)
    private String eventName;

    // 구매자명 검색 (부분 검색)
    private String buyerName;

    // 결제 금액 범위
    private Long minAmount;
    private Long maxAmount;

    // 페이징 및 정렬
    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;

    @Builder.Default
    private String sort = "paidAt"; // paidAt, amount, eventName, buyerName

    @Builder.Default
    private String direction = "desc"; // asc, desc
}