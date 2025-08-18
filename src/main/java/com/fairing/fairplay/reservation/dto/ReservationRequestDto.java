package com.fairing.fairplay.reservation.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReservationRequestDto {

    private Long eventId;
    private Long scheduleId;
    private Long ticketId;
    private Long reservationId;

    private int quantity;
    private int price;
    private boolean canceled;
    
    private String paymentMethod;
    
    // 결제 정보 (아임포트)
    private PaymentData paymentData;
    
    @Getter
    @Setter
    public static class PaymentData {
        private String imp_uid;        // 아임포트 결제 고유번호
        private String merchant_uid;   // 가맹점 주문번호
        private int paid_amount;       // 실제 결제금액
        private String apply_num;      // 카드 승인번호
    }
}
