package com.fairing.fairplay.payment.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundListResponseDto {
    
    private Long refundId;
    private Long paymentId;
    private String merchantUid;
    
    // 이벤트 정보
    private Long eventId;
    private String eventName;
    
    // 사용자 정보
    private Long userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    
    // 결제 정보
    private String paymentTargetType; // RESERVATION, BOOTH, AD
    private String paymentTargetName; // 예약, 부스, 광고
    private Long targetId;
    private Integer quantity;
    private BigDecimal price; // 개당 가격
    private BigDecimal totalAmount; // 총 결제 금액
    private LocalDateTime paidAt;
    
    // 환불 정보
    private BigDecimal refundAmount; // 환불 금액
    private String refundReason;
    private String refundStatus; // REQUESTED, APPROVED, REJECTED
    private String refundStatusName; // 요청, 승인, 거부
    private LocalDateTime refundCreatedAt;
    private LocalDateTime refundApprovedAt;
}