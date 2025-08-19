package com.fairing.fairplay.payment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundListRequestDto {
    
    private String eventName; // 이벤트명으로 검색
    private String paymentDateFrom; // 결제일자 범위 시작 (yyyy-MM-dd)
    private String paymentDateTo; // 결제일자 범위 끝 (yyyy-MM-dd)
    private String refundStatus; // 환불 상태 (REQUESTED, APPROVED, REJECTED)
    private String paymentTargetType; // 결제 대상 타입 (RESERVATION, BOOTH, AD)
    
    @Builder.Default
    private int page = 0;
    
    @Builder.Default
    private int size = 20;
    
    @Builder.Default
    private String sortBy = "createdAt";
    
    @Builder.Default
    private String sortDirection = "desc";
}