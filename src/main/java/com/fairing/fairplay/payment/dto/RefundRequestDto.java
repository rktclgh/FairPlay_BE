package com.fairing.fairplay.payment.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequestDto {
    
    private Long paymentId; // 환불 대상 결제 ID
    private BigDecimal refundAmount; // 환불 요청 금액
    private Integer refundQuantity; // 환불 요청 수량 (부분 환불용)
    private String reason; // 환불 사유
    private String refundType; // 환불 유형 (FULL: 전체, PARTIAL: 부분)
}