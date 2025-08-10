package com.fairing.fairplay.payment.dto;

import com.fairing.fairplay.payment.entity.Refund;
import com.fairing.fairplay.payment.entity.RefundStatusCode;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponseDto {

    private Long refundId;
    private Long paymentId;
    private Long eventId;
    private Long userId;
    private String merchantUid;
    private BigDecimal refundAmount;
    private String reason;
    private String statusCode;        // 환불 상태 코드 (REQUESTED, APPROVED, REJECTED)
    private String statusName;        // 환불 상태 이름 (요청, 승인, 거부)
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    // 결제 관련 정보
    private BigDecimal originalAmount;
    private Integer originalQuantity;
    private BigDecimal price;
    private String eventTitle;
    private String userName;

    public static RefundResponseDto fromEntity(Refund refund) {
        return RefundResponseDto.builder()
                .refundId(refund.getRefundId())
                .paymentId(refund.getPayment().getPaymentId())
                .eventId(refund.getPayment().getEvent().getEventId())
                .userId(refund.getPayment().getUser().getUserId())
                .merchantUid(refund.getPayment().getMerchantUid())
                .refundAmount(refund.getAmount())
                .reason(refund.getReason())
                .statusCode(refund.getRefundStatusCode().getCode())
                .statusName(refund.getRefundStatusCode().getName())
                .createdAt(refund.getCreatedAt())
                .approvedAt(refund.getApprovedAt())
                .originalAmount(refund.getPayment().getAmount())
                .originalQuantity(refund.getPayment().getQuantity())
                .price(refund.getPayment().getPrice())
                // 필요시 Event와 User 정보 추가
                // .eventTitle(refund.getPayment().getEvent().getTitle())
                // .userName(refund.getPayment().getUser().getName())
                .build();
    }

    public static List<RefundResponseDto> fromEntityList(List<Refund> refunds) {
        return refunds.stream()
                .map(RefundResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
}