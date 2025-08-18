package com.fairing.fairplay.reservation.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.repository.PaymentRepository;
import com.fairing.fairplay.reservation.dto.ReservationResponseDto;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me/reservations")
@RequiredArgsConstructor
public class MyReservationController {

    private final ReservationService reservationService;
    private final PaymentRepository paymentRepository;

    @GetMapping
    public ResponseEntity<List<ReservationResponseDto>> getMyReservations(@AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        List<Reservation> myReservations = reservationService.getMyReservations(userId);

        List<ReservationResponseDto> response = myReservations.stream()
                .map(reservation -> {
                    ReservationResponseDto dto = ReservationResponseDto.from(reservation);
                    
                    // 해당 예약과 연결된 결제 정보 조회
                    Payment payment = paymentRepository.findByTargetIdAndPaymentTargetType_PaymentTargetCode(
                            reservation.getReservationId(), "RESERVATION").orElse(null);
                    
                    if (payment != null) {
                        dto.setPaymentId(payment.getPaymentId());
                        dto.setMerchantUid(payment.getMerchantUid());
                        dto.setImpUid(payment.getImpUid());
                        dto.setPaymentAmount(payment.getAmount());
                        dto.setPaymentStatus(payment.getPaymentStatusCode().getName());
                        dto.setPaymentMethod(payment.getPaymentTypeCode().getName());
                        dto.setPaidAt(payment.getPaidAt());
                    }
                    
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(response);
    }
}
