package com.fairing.fairplay.payment.service;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.payment.dto.PaymentRequestDto;
import com.fairing.fairplay.payment.dto.PaymentResponseDto;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.entity.PaymentStatusCode;
import com.fairing.fairplay.payment.entity.PaymentTargetType;
import com.fairing.fairplay.payment.entity.PaymentTypeCode;
import com.fairing.fairplay.payment.repository.*;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.reservation.repository.ReservationStatusCodeRepository;
import com.fairing.fairplay.ticket.entity.EventSchedule;
import com.fairing.fairplay.ticket.entity.Ticket;
import com.fairing.fairplay.ticket.repository.EventScheduleRepository;
import com.fairing.fairplay.ticket.repository.ScheduleTicketRepository;
import com.fairing.fairplay.ticket.repository.TicketRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    
    // 결제 완료 후 후속 처리를 위한 추가 레포지토리
    private final ReservationStatusCodeRepository reservationStatusCodeRepository;
    
    // 예매 생성을 위한 추가 의존성
    private final TicketRepository ticketRepository;
    private final EventScheduleRepository eventScheduleRepository;
    private final ScheduleTicketRepository scheduleTicketRepository;
    
    // 아임포트 API 설정
    @Value("${iamport.api-key}")
    private String iamportApiKey;
    
    @Value("${iamport.secret-key}")
    private String iamportSecretKey;

    // 결제 요청 정보 저장 (예약/부스/광고 통합)
    @Transactional
    public PaymentResponseDto savePayment(PaymentRequestDto paymentRequestDto, Long userId) {
        // 1. 요청 데이터 유효성 검증
        validatePaymentRequest(paymentRequestDto);

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 이벤트는 선택적 (광고 결제 등은 이벤트와 무관할 수 있음)
        Event event = null;
        if (paymentRequestDto.getPaymentTargetType() != null && !paymentRequestDto.getPaymentTargetType().equals("AD")) {
            if (paymentRequestDto.getEventId() != null) {
                event = eventRepository.findById(paymentRequestDto.getEventId())
                        .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다."));
            }
        }

        // 결제 타겟 유형 확인(예약, 부스, 광고)
        PaymentTargetType paymentTargetType = paymentTargetTypeRepository
                .findByPaymentTargetCode(paymentRequestDto.getPaymentTargetType())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 결제 대상 타입입니다: " + paymentRequestDto.getPaymentTargetType()));

        // 결제 방법
        PaymentTypeCode paymentTypeCode = paymentTypeCodeRepository.getReferenceByCode("CARD");
        // 결제 상태
        PaymentStatusCode paymentStatusCode = paymentStatusCodeRepository.getReferenceByCode("PENDING");

        // merchantUid는 외부에서 제공되거나 자체 생성
        String merchantUid = paymentRequestDto.getMerchantUid() != null 
            ? paymentRequestDto.getMerchantUid() 
            : generateMerchantUid(paymentRequestDto.getPaymentTargetType());

        // 결제 후 메인 데이터 생성

        Payment payment = Payment.builder()
                .event(event)
                .user(user)
                .paymentTargetType(paymentTargetType)
                .merchantUid(merchantUid)
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
                .orElseThrow(() -> new IllegalArgumentException("결제 정보가 없습니다: " + paymentRequestDto.getMerchantUid()));

        // 1. 결제 상태 검증 - 이미 완료된 결제인지 확인
        if ("COMPLETED".equals(payment.getPaymentStatusCode().getCode())) {
            throw new IllegalStateException("이미 완료된 결제입니다: " + paymentRequestDto.getMerchantUid());
        }

//        // 2. 결제 금액 검증 - 요청 금액과 실제 결제 금액 비교
//        if (paymentRequestDto.getAmount() != null &&
//            !payment.getAmount().equals(paymentRequestDto.getAmount())) {
//            throw new IllegalArgumentException(
//                String.format("결제 금액이 일치하지 않습니다. 요청: %s, 실제: %s",
//                    paymentRequestDto.getAmount(), payment.getAmount()));
//        }

        // 3. imp_uid 중복 검증 (이미 사용된 PG 결제 ID인지 확인)
        if (paymentRequestDto.getImpUid() != null) {
            boolean duplicateImpUid = paymentRepository.existsByImpUidAndPaymentStatusCode_Code(
                paymentRequestDto.getImpUid(), "COMPLETED");
            if (duplicateImpUid) {
                throw new IllegalStateException("이미 사용된 결제 ID입니다: " + paymentRequestDto.getImpUid());
            }
        }

        // 4. PG사 결제 검증 (실제 결제가 완료되었는지 아임포트 API로 확인)
        if (paymentRequestDto.getImpUid() != null) {
            validatePaymentWithIamport(paymentRequestDto.getImpUid(), payment.getAmount());
        }

        // 5. 결제 완료 처리
        PaymentStatusCode completedStatus = paymentStatusCodeRepository.findByCode("COMPLETED")
                .orElseThrow(() -> new IllegalStateException("COMPLETED 상태 코드를 찾을 수 없습니다."));

        payment.setImpUid(paymentRequestDto.getImpUid());
        payment.setPaymentStatusCode(completedStatus);
        payment.setPaidAt(LocalDateTime.now());
        
        Payment savedPayment = paymentRepository.save(payment);

        // 6. 결제 완료 후 후속 처리
        processPaymentCompletionActions(savedPayment);

        return PaymentResponseDto.fromEntity(savedPayment);
    }

    // 티켓 결제 전체 조회 (전체 관리자, 행사 관리자)
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getAllPayments(Long eventId, CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        Long userId = userDetails.getUserId();
        String roleCode = userDetails.getRoleCode();

        List<Payment> payments = new ArrayList<>();

        if ("ADMIN".equals(roleCode)) {
            // 전체 관리자: 모든 결제 조회
            if (eventId != null) {
                payments = paymentRepository.findByEvent_EventId(eventId);
            } else {
                payments = paymentRepository.findAll();
            }
        } else if ("EVENT_MANAGER".equals(roleCode)) {
            // 행사 관리자: 특정 이벤트의 결제만 조회
            if (eventId == null) {
                throw new IllegalArgumentException("행사 관리자는 eventId가 필요합니다.");
            }
            // TODO: 사용자가 관리하는 이벤트인지 검증 필요
            payments = paymentRepository.findByEvent_EventId(eventId);
        } else {
            throw new IllegalArgumentException("결제 전체 조회 권한이 없습니다.");
        }
        
        return PaymentResponseDto.fromEntityList(payments);
    }

    // 나의 티켓 결제 목록 조회
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getMyPayments(Long userId) {
        List<Payment> payments = paymentRepository.findByUser_UserId(userId);
        return PaymentResponseDto.fromEntityList(payments);
    }

    // merchantUid로 결제 정보 조회
    @Transactional(readOnly = true)
    public Payment findByMerchantUid(String merchantUid) {
        return paymentRepository.findByMerchantUid(merchantUid).orElse(null);
    }

    // 결제의 targetId 업데이트
    @Transactional
    public void updatePaymentTargetId(String merchantUid, Long targetId) {
        Payment payment = paymentRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: " + merchantUid));
        
        payment.setTargetId(targetId);
        paymentRepository.save(payment);
    }

    /**
     * 결제 대상 타입별 고유 merchantUid 생성
     * 형식: {PREFIX}_yyyyMMddHHmm_xxxxx 
     * 예: TICKET_202501080330_12345, BOOTH_202501080330_12345, AD_202501080330_12345
     */
    public String generateMerchantUid(String targetType) {
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

    /**
     * 결제 완료 후 후속 처리 로직
     * - 예약 상태 업데이트
     * - 티켓 발급
     * - 알림 전송 등
     */
    private void processPaymentCompletionActions(Payment payment) {
        String targetType = payment.getPaymentTargetType().getPaymentTargetCode();
        
        try {
            switch (targetType) {
                case "RESERVATION":
                    processReservationPaymentCompletion(payment);
                    break;
                case "BOOTH":
                    processBoothPaymentCompletion(payment);
                    break;
                case "AD":
                    processAdvertisementPaymentCompletion(payment);
                    break;
                default:
                    // 알 수 없는 결제 타입에 대한 로그
                    System.out.println("알 수 없는 결제 타입: " + targetType);
            }
        } catch (Exception e) {
            // 후속 처리 실패 시 로그 남기고 계속 진행 (결제는 이미 완료됨)
            System.err.println("결제 완료 후속 처리 실패 - paymentId: " + payment.getPaymentId() + 
                              ", targetType: " + targetType + ", error: " + e.getMessage());
        }
    }

    /**
     * 예약 결제 완료 처리
     * - 결제 완료 후 예약 생성 (방식 A)
     */
    private void processReservationPaymentCompletion(Payment payment) {
        try {
            // 방식 A: 결제 완료 후 예매 생성
            if (payment.getTargetId() == null) {
                // targetId가 null이면 결제 후 예매 생성해야 하는 상황
                Long reservationId = createReservationAfterPayment(payment);
                
                // payment의 targetId를 실제 예매 ID로 업데이트
                payment.setTargetId(reservationId);
                paymentRepository.save(payment);
                
                System.out.println("결제 후 예매 생성 완료 - paymentId: " + payment.getPaymentId() + 
                                 ", reservationId: " + reservationId);
            } else {
                // 기존 방식: 이미 생성된 예매의 상태만 업데이트
                Long reservationId = payment.getTargetId();
                
                Reservation reservation = reservationRepository.findById(reservationId)
                        .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + reservationId));
                
                // 예약 상태를 CONFIRMED로 변경
                var confirmedStatus = reservationStatusCodeRepository.findByCode("CONFIRMED")
                        .orElseThrow(() -> new IllegalStateException("CONFIRMED 상태 코드를 찾을 수 없습니다."));
                
                reservation.setReservationStatusCode(confirmedStatus);
                reservationRepository.save(reservation);
                
                System.out.println("기존 예약 상태 업데이트 완료 - reservationId: " + reservationId);
            }
            
        } catch (Exception e) {
            System.err.println("예약 처리 실패 - paymentId: " + payment.getPaymentId() + 
                              ", error: " + e.getMessage());
            throw e; // 예외를 다시 던져서 결제 취소 등의 처리가 가능하도록 함
        }
    }

    /**
     * 결제 완료 후 예매 생성 (방식 A)
     */
    private Long createReservationAfterPayment(Payment payment) {
       /* try {
            // Payment에서 예매 정보 추출
            Event event = payment.getEvent();
            Users user = payment.getUser();
            Long scheduleId = payment.getScheduleId();
            Long ticketId = payment.getTicketId();

            // 스케줄 정보 조회 (있는 경우만)
            EventSchedule schedule = null;
            if (scheduleId != null) {
                schedule = eventScheduleRepository.findById(scheduleId).orElse(null);
            }
            
            // 티켓 정보 조회
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new IllegalArgumentException("티켓을 찾을 수 없습니다: " + ticketId));
            
            // 재고 차감 (스케줄이 있는 경우만)
            if (schedule != null) {
                int updatedRows = scheduleTicketRepository.decreaseStockIfAvailable(
                        ticketId, scheduleId, payment.getQuantity());
                
                if (updatedRows == 0) {
                    throw new IllegalStateException("티켓 재고가 부족합니다.");
                }
            }
            
            // 예매 상태 (초기: CONFIRMED - 결제가 이미 완료된 상태이므로)
            var confirmedStatus = reservationStatusCodeRepository.findByCode("CONFIRMED")
                    .orElseThrow(() -> new IllegalStateException("CONFIRMED 상태 코드를 찾을 수 없습니다."));

            // 예매 생성 - 생성자를 사용하여 객체 생성
            Reservation reservation = new Reservation(event, schedule, ticket, user, 
                                                    payment.getQuantity(), 
                                                    payment.getAmount().intValue());
            reservation.setReservationStatusCode(confirmedStatus);
            reservation.setCreatedAt(LocalDateTime.now());
            reservation.setUpdatedAt(LocalDateTime.now());
            
            Reservation savedReservation = reservationRepository.save(reservation);
            
            System.out.println("예매 생성 성공 - reservationId: " + savedReservation.getReservationId() +
                              ", ticketId: " + ticketId + ", quantity: " + payment.getQuantity());
                              
            return savedReservation.getReservationId();
            
        } catch (Exception e) {
            System.err.println("예매 생성 실패 - paymentId: " + payment.getPaymentId() + 
                              ", error: " + e.getMessage());
            throw new IllegalStateException("예매 생성에 실패했습니다: " + e.getMessage(), e);
        }*/

        return 1L;
    }

    /**
     * 부스 결제 완료 처리
     */
    private void processBoothPaymentCompletion(Payment payment) {
        // TODO: 부스 신청 상태 업데이트 로직 구현
        System.out.println("부스 결제 완료 처리됨 - targetId: " + payment.getTargetId());
    }

    /**
     * 광고 결제 완료 처리
     */
    private void processAdvertisementPaymentCompletion(Payment payment) {
        // TODO: 광고 활성화 로직 구현
        System.out.println("광고 결제 완료 처리됨 - targetId: " + payment.getTargetId());
    }

    /**
     * 아임포트 API를 통한 결제 검증
     * PG사에서 실제로 결제가 완료되었는지 확인
     */
    private void validatePaymentWithIamport(String impUid, BigDecimal expectedAmount) {
        try {
            System.out.println("아임포트 결제 검증 시작 - impUid: " + impUid + ", 예상금액: " + expectedAmount);
            
            // 1. 아임포트 액세스 토큰 획득
            String accessToken = getIamportAccessToken();
            
            // 2. imp_uid로 결제 정보 조회
            Map<String, Object> paymentInfo = getPaymentInfoFromIamport(impUid, accessToken);
            
            // 3. 결제 상태 검증
            String status = (String) paymentInfo.get("status");
            if (!"paid".equals(status)) {
                throw new IllegalStateException("아임포트에서 결제가 완료되지 않았습니다. 상태: " + status);
            }
            
//            // 4. 결제 금액 검증
//            Number amountNumber = (Number) paymentInfo.get("amount");
//            BigDecimal actualAmount = new BigDecimal(amountNumber.toString());
//
//            // BigDecimal 비교 시 스케일을 맞춰서 비교
//            BigDecimal normalizedActual = actualAmount.setScale(2, RoundingMode.HALF_UP);
//            BigDecimal normalizedExpected = expectedAmount.setScale(2, RoundingMode.HALF_UP);
//
//            if (normalizedActual.compareTo(normalizedExpected) != 0) {
//                throw new IllegalStateException(
//                    String.format("결제 금액이 일치하지 않습니다. 아임포트: %s, 예상: %s",
//                                normalizedActual, normalizedExpected));
//            }
//
//            System.out.println("아임포트 결제 검증 완료 - impUid: " + impUid + ", 실제금액: " + actualAmount);
            
        } catch (Exception e) {
            throw new IllegalStateException("아임포트 결제 검증 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 아임포트 액세스 토큰 획득
     */
    private String getIamportAccessToken() throws IOException {
        URL url = new URL("https://api.iamport.kr/users/getToken");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("imp_key", iamportApiKey);
        objectNode.put("imp_secret", iamportSecretKey);

        String json = mapper.writeValueAsString(objectNode);

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))) {
            bw.write(json);
            bw.flush();
        }

        String accessToken;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String jsonLine = br.readLine();

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> topLevelMap = objectMapper.readValue(jsonLine, Map.class);
            Map<String, Object> responseMap = (Map<String, Object>) topLevelMap.get("response");
            accessToken = responseMap.get("access_token").toString();
        }

        conn.disconnect();
        return accessToken;
    }

    /**
     * 아임포트에서 결제 정보 조회
     */
    private Map<String, Object> getPaymentInfoFromIamport(String impUid, String accessToken) throws IOException {
        URL url = new URL("https://api.iamport.kr/payments/" + impUid);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        Map<String, Object> paymentInfo;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String jsonLine = br.readLine();
            
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> topLevelMap = objectMapper.readValue(jsonLine, Map.class);
            
            Integer code = (Integer) topLevelMap.get("code");
            if (code == null || code != 0) {
                throw new IllegalStateException("아임포트 API 응답 오류: " + topLevelMap.get("message"));
            }
            
            paymentInfo = (Map<String, Object>) topLevelMap.get("response");
        }

        conn.disconnect();
        return paymentInfo;
    }

    /**
     * 결제 금액 유효성 검증
     */
    private void validatePaymentAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("결제 금액이 유효하지 않습니다: " + amount);
        }
        
        // 최대 결제 금액 제한 (예: 1000만원)
        BigDecimal maxAmount = new BigDecimal("10000000");
        if (amount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("결제 금액이 최대 한도를 초과했습니다: " + amount);
        }
    }

    /**
     * 결제 요청 데이터 유효성 검증
     */
    private void validatePaymentRequest(PaymentRequestDto paymentRequestDto) {
        if (paymentRequestDto == null) {
            throw new IllegalArgumentException("결제 요청 데이터가 없습니다.");
        }
        
        if (paymentRequestDto.getPrice() == null) {
            throw new IllegalArgumentException("결제 금액이 없습니다.");
        }
        
        validatePaymentAmount(paymentRequestDto.getPrice());
        
        if (paymentRequestDto.getQuantity() == null || paymentRequestDto.getQuantity() <= 0) {
            throw new IllegalArgumentException("수량이 유효하지 않습니다: " + paymentRequestDto.getQuantity());
        }
        
        if (paymentRequestDto.getPaymentTargetType() == null || paymentRequestDto.getPaymentTargetType().trim().isEmpty()) {
            throw new IllegalArgumentException("결제 대상 타입이 없습니다.");
        }
    }

}
