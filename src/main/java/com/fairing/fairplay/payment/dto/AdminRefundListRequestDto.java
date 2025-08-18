package com.fairing.fairplay.payment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRefundListRequestDto {
    
    private String eventName;           // 이벤트명 필터
    private String userName;            // 사용자명 필터
    private String paymentDateFrom;     // 결제일 시작
    private String paymentDateTo;       // 결제일 종료
    private String refundStatus;        // 환불 상태 (REQUESTED, APPROVED, REJECTED, COMPLETED, FAILED)
    private String paymentTargetType;   // 결제 유형 (RESERVATION, BOOTH, AD)
    private Long eventId;               // 특정 이벤트 ID (이벤트 관리자용)
    
    @Builder.Default
    private int page = 0;               // 페이지 번호
    
    @Builder.Default
    private int size = 20;              // 페이지 크기
    
    @Builder.Default
    private String sortBy = "refundCreatedAt";  // 정렬 기준
    
    @Builder.Default
    private String sortDirection = "desc";      // 정렬 방향
}