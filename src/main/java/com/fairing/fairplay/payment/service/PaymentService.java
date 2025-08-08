package com.fairing.fairplay.payment.service;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.payment.dto.*;
import com.fairing.fairplay.payment.entity.*;
import com.fairing.fairplay.payment.repository.PaymentRepository;
import com.fairing.fairplay.payment.repository.PaymentStatusCodeRepository;
import com.fairing.fairplay.payment.repository.PaymentTargetTypeRepository;
import com.fairing.fairplay.payment.repository.PaymentTypeCodeRepository;
import com.fairing.fairplay.payment.repository.RefundRepository;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    private final PaymentStatusCodeRepository paymentStatusCodeRepository;
    private final PaymentTypeCodeRepository paymentTypeCodeRepository;
    private final PaymentTargetTypeRepository paymentTargetTypeRepository;

    // 결제 요청 정보 저장 (예약/부스/광고 통합)
    @Transactional
    public PaymentResponseDto savePayment(PaymentRequestDto paymentRequestDto, Long userId) {

        Event event = eventRepository.findById(paymentRequestDto.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다."));
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        PaymentTargetType paymentTargetType = paymentTargetTypeRepository
                .findByPaymentTargetCode(paymentRequestDto.getTargetType())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 결제 대상 타입입니다: " + paymentRequestDto.getTargetType()));

        PaymentTypeCode paymentTypeCode = paymentTypeCodeRepository.getReferenceById(1);
        PaymentStatusCode paymentStatusCode = paymentStatusCodeRepository.getReferenceById(1);

        // 예약인 경우 유효성 검증 (실제 예약이 존재하는지 확인)
        if ("RESERVATION".equals(paymentRequestDto.getTargetType())) {
            if (paymentRequestDto.getTargetId() == null) {
                throw new IllegalArgumentException("예약 ID가 필요합니다.");
            }
            reservationRepository.findById(paymentRequestDto.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));
        }

        Payment payment = Payment.builder()
                .event(event)
                .user(user)
                .paymentTargetType(paymentTargetType)
                .targetId(paymentRequestDto.getTargetId())
                .merchantUid(generateMerchantUid(paymentRequestDto.getTargetType()))
                .quantity(paymentRequestDto.getQuantity())
                .price(paymentRequestDto.getPrice())
                .amount(paymentRequestDto.getPrice().multiply(new BigDecimal(paymentRequestDto.getQuantity())))
                .pgProvider(paymentRequestDto.getPgProvider())
                .paymentTypeCode(paymentTypeCode)
                .paymentStatusCode(paymentStatusCode) // 초기 상태: PENDING
                .requestedAt(LocalDateTime.now())
                .build();

        Payment saved = paymentRepository.save(payment);
        return PaymentResponseDto.fromEntity(saved);
    }

    // 티켓 결제 완료 처리 (PG사 결제 후 호출)
    @Transactional
    public PaymentResponseDto completePayment(PaymentRequestDto paymentRequestDto) {

        Payment payment = paymentRepository.findByMerchantUid(paymentRequestDto.getMerchantUid())
                .orElseThrow(() -> new IllegalArgumentException("결제 정보가 없습니다."));

        PaymentStatusCode paymentStatusCode = paymentStatusCodeRepository.getReferenceById(2);

        payment.setImpUid(paymentRequestDto.getImpUid());
        payment.setPaymentStatusCode(paymentStatusCode); // 결제 상태
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return PaymentResponseDto.fromEntity(payment);
    }

    // 티켓 결제 전체 조회 (전체 관리자, 행사 관리자)
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getAllPayments(Long eventId, CustomUserDetails userDetails) {
        // 임시 하드코딩: userId = 1L, roleCode = "ADMIN"
        Long userId = 1L;
        String roleCode = "ADMIN";
        // if(userDetails != null) {
        //     userId = userDetails.getUserId();
        //     roleCode = userDetails.getRoleCode();
        // }

        List<Payment> payments = new ArrayList<>();

        if (roleCode != null && roleCode.equals("ADMIN")) {
            payments = paymentRepository.findAll();
        } else if (eventId != null) {
            payments = paymentRepository.findByEvent_EventId(eventId);
        }
        return PaymentResponseDto.fromEntityList(payments);
    }

    // 나의 티켓 결제 목록 조회
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getMyPayments(Long userId) {
        List<Payment> payments = paymentRepository.findByUser_UserId(userId);
        return PaymentResponseDto.fromEntityList(payments);
    }

    /**
     * 결제 대상 타입별 고유 merchantUid 생성
     * 형식: {PREFIX}_yyyyMMddHHmm_xxxxx 
     * 예: TICKET_202501080330_12345, BOOTH_202501080330_12345, AD_202501080330_12345
     */
    private String generateMerchantUid(String targetType) {
        // 결제 대상 타입별 접두사 설정
        String prefix;
        switch (targetType) {
            case "RESERVATION":
                prefix = "TICKET";
                break;
            case "BOOTH":
                prefix = "BOOTH";
                break;
            case "AD":
                prefix = "AD";
                break;
            default:
                prefix = "PAY";
        }
        
        // 현재 시간을 yyyyMMddHHmm 형식으로 포맷
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        
        // 5자리 랜덤 숫자 생성 (10000~99999)
        Random random = new Random();
        int randomNum = 10000 + random.nextInt(90000);
        
        return String.format("%s_%s_%d", prefix, timestamp, randomNum);
    }

}
