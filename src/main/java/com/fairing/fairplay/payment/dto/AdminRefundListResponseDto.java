package com.fairing.fairplay.payment.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRefundListResponseDto {
    
    // 환불 정보
    private Long refundId;
    private BigDecimal refundAmount;
    private String refundReason;
    private String refundStatus;
    private String refundStatusName;
    private LocalDateTime refundCreatedAt;
    private LocalDateTime refundApprovedAt;
    private String adminComment;
    private String approvedByName;      // 승인자 이름
    
    // 결제 정보
    private Long paymentId;
    private String merchantUid;
    private String impUid;
    private BigDecimal paymentAmount;
    private Integer quantity;
    private BigDecimal price;
    private String paymentTargetType;
    private String paymentTargetName;
    private LocalDateTime paidAt;
    
    // 이벤트 정보
    private Long eventId;
    private String eventName;
    private LocalDate eventStartDate;
    private LocalDate eventEndDate;
    
    // 사용자 정보
    private Long userId;
    private String userName;
    private String userEmail;
    private String userPhone;
}