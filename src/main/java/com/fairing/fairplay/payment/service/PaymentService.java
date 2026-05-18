package com.fairing.fairplay.payment.service;

import com.fairing.fairplay.attendeeform.dto.AttendeeFormSaveRequestDto;
import com.fairing.fairplay.attendeeform.dto.AttendeeFormSaveResponseDto;
import com.fairing.fairplay.banner.entity.BannerApplication;
import com.fairing.fairplay.banner.repository.BannerApplicationRepository;
import com.fairing.fairplay.booth.entity.Booth;
import com.fairing.fairplay.booth.entity.BoothApplication;
import com.fairing.fairplay.booth.repository.BoothApplicationRepository;
import com.fairing.fairplay.booth.repository.BoothRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.email.service.PaymentCompletionEmailService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.notification.dto.NotificationRequestDto;
import com.fairing.fairplay.notification.service.NotificationService;
import com.fairing.fairplay.payment.dto.PaymentRequestDto;
import com.fairing.fairplay.payment.dto.PaymentResponseDto;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.entity.PaymentStatusCode;
import com.fairing.fairplay.payment.entity.PaymentTargetType;
import com.fairing.fairplay.payment.entity.PaymentTypeCode;
import com.fairing.fairplay.payment.repository.*;
import com.fairing.fairplay.reservation.dto.ReservationRequestDto;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.reservation.repository.ReservationStatusCodeRepository;
import com.fairing.fairplay.reservation.service.ReservationService;
import com.fairing.fairplay.attendeeform.service.AttendeeFormAttendeeService;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

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

    // 알림 서비스
    private final NotificationService notificationService;

    // 결제 완료 이메일 서비스
    private final PaymentCompletionEmailService paymentCompletionEmailService;

    // 예약 서비스 (티켓 생성용)
    private final ReservationService reservationService;

    // 부스 신청 정보 조회를 위한 레포지토리
    private final BoothApplicationRepository boothApplicationRepository;
    private final BoothRepository boothRepository;
    
    // 배너 신청 정보 조회를 위한 레포지토리
    private final BannerApplicationRepository bannerApplicationRepository;

    // 참석자 생성 및 폼 링크 생성
    private final AttendeeFormAttendeeService attendeeFormAttendeeService;

    // 아임포트 API 설정
    @Value("${iamport.api-key}")
    private String iamportApiKey;

    @Value("${iamport.secret-key}")
    private String iamportSecretKey;

    // 결제 요청 정보 저장 (예약/부스/광고 통합)
    @Transactional
    public PaymentResponseDto savePayment(PaymentRequestDto paymentRequestDto, Long userId) {
        // 디버깅용 로그
        System.out.println("🔵 [PaymentService] savePayment - scheduleId: " + paymentRequestDto.getScheduleId() + 
                ", ticketId: " + paymentRequestDto.getTicketId());
        
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
        validatePaymentTargetCreationPermission(paymentTargetType, paymentRequestDto.getTargetId(), user);

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
                .targetId(paymentRequestDto.getTargetId()) // DTO에서 제공된 경우에만 설정
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

    // 무료 티켓 직접 처리 (PG사 연동 없음)
    @Transactional
    public PaymentResponseDto processFreeTicket(PaymentRequestDto paymentRequestDto, Long userId) {
        // 1. 요청 데이터 유효성 검증
        validatePaymentRequest(paymentRequestDto);

        // 2. 무료 티켓인지 확인
        BigDecimal totalAmount = paymentRequestDto.getPrice().multiply(new BigDecimal(paymentRequestDto.getQuantity()));
        if (totalAmount.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("무료 티켓이 아닙니다. 금액: " + totalAmount);
        }

        // 3. 결제 정보 저장 (PENDING 상태)
        PaymentResponseDto savedPayment = savePayment(paymentRequestDto, userId);

        // 4. 즉시 완료 처리 (PG사 연동 없이)
        Payment payment = paymentRepository.findByMerchantUid(paymentRequestDto.getMerchantUid())
                .orElseThrow(() -> new IllegalArgumentException("결제 정보가 없습니다: " + paymentRequestDto.getMerchantUid()));

        // 5. 결제 완료 상태로 변경
        PaymentStatusCode completedStatus = paymentStatusCodeRepository.findByCode("COMPLETED")
                .orElseThrow(() -> new IllegalStateException("COMPLETED 상태 코드를 찾을 수 없습니다."));

        payment.setPaymentStatusCode(completedStatus);
        payment.setPaidAt(LocalDateTime.now());
        // 무료 티켓의 경우 imp_uid는 "FREE_" + merchantUid 형태로 설정
        payment.setImpUid("FREE_" + payment.getMerchantUid());

        Payment savedPaymentEntity = paymentRepository.save(payment);

        // 6. 결제 완료 후 후속 처리 (예약 생성 등) - PaymentRequestDto의 scheduleId, ticketId 전달
        processPaymentCompletionActions(savedPaymentEntity, paymentRequestDto.getScheduleId(), paymentRequestDto.getTicketId());

        return PaymentResponseDto.fromEntity(savedPaymentEntity);
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

        // 6. 결제 완료 후 후속 처리 (PaymentRequestDto의 scheduleId, ticketId 사용)
        System.out.println("🔵 [PaymentService] completePayment - 받은 scheduleId: " + paymentRequestDto.getScheduleId() + 
                ", ticketId: " + paymentRequestDto.getTicketId());
        processPaymentCompletionActions(savedPayment, paymentRequestDto.getScheduleId(), paymentRequestDto.getTicketId());

        // 7. 후속 처리로 업데이트된 payment 정보를 다시 조회하여 반환
        Payment updatedPayment = paymentRepository.findById(savedPayment.getPaymentId())
                .orElse(savedPayment);
        
        System.out.println("🟢 [PaymentService] completePayment 반환 - paymentId: " + updatedPayment.getPaymentId() +
                ", targetId: " + updatedPayment.getTargetId());
        
        return PaymentResponseDto.fromEntity(updatedPayment);
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
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다."));
            if (!isManagedBy(event, userId)) {
                throw new AccessDeniedException("담당 행사 결제만 조회할 수 있습니다.");
            }
            payments = paymentRepository.findByEvent_EventId(eventId);
        } else {
            throw new AccessDeniedException("결제 전체 조회 권한이 없습니다.");
        }

        return PaymentResponseDto.fromEntityList(payments);
    }

    // 나의 티켓 결제 목록 조회
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getMyPayments(Long userId) {
        List<Payment> payments = paymentRepository.findByUserIdWithEventInfo(userId);
        
        // 환불된 결제 제외
        List<Payment> activePayments = payments.stream()
                .filter(payment -> !"REFUNDED".equals(payment.getPaymentStatusCode().getCode()))
                .toList();
        
        return PaymentResponseDto.fromEntityList(activePayments);
    }

    // merchantUid로 결제 정보 조회
    @Transactional(readOnly = true)
    public Payment findByMerchantUid(String merchantUid) {
        return paymentRepository.findByMerchantUid(merchantUid).orElse(null);
    }

    // 결제의 targetId 업데이트
    @Transactional
    public void updatePaymentTargetId(String merchantUid, Long targetId, CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new AccessDeniedException("인증되지 않은 사용자입니다.");
        }

        Payment payment = paymentRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: " + merchantUid));

        validatePaymentTargetUpdatePermission(payment, targetId, userDetails);

        payment.setTargetId(targetId);
        paymentRepository.save(payment);
    }

    private void validatePaymentTargetUpdatePermission(Payment payment, Long requestedTargetId, CustomUserDetails userDetails) {
        Long currentTargetId = payment.getTargetId();
        if (currentTargetId != null && !Objects.equals(currentTargetId, requestedTargetId)) {
            throw new AccessDeniedException("이미 다른 대상에 연결된 결제입니다.");
        }

        String roleCode = userDetails.getRoleCode();
        if ("ADMIN".equals(roleCode)) {
            return;
        }

        if ("COMMON".equals(roleCode)) {
            Long paymentUserId = payment.getUser() != null ? payment.getUser().getUserId() : null;
            if (Objects.equals(paymentUserId, userDetails.getUserId())) {
                validateCommonTargetOwnership(payment, requestedTargetId, userDetails.getUserId());
                return;
            }
            throw new AccessDeniedException("본인 결제만 대상 정보를 수정할 수 있습니다.");
        }

        throw new AccessDeniedException("결제 대상 정보 수정 권한이 없습니다.");
    }

    private void validateCommonTargetOwnership(Payment payment, Long requestedTargetId, Long principalUserId) {
        if (requestedTargetId == null) {
            throw new AccessDeniedException("결제 대상 정보가 필요합니다.");
        }

        String targetCode = payment.getPaymentTargetType() != null
                ? payment.getPaymentTargetType().getPaymentTargetCode()
                : null;

        switch (targetCode) {
            case "RESERVATION" -> validateReservationOwner(requestedTargetId, principalUserId);
            case "BANNER_APPLICATION" -> validateBannerApplicationOwner(requestedTargetId, principalUserId);
            case "BOOTH_APPLICATION" -> validateBoothApplicationOwner(requestedTargetId, principalUserId);
            case "BOOTH", "AD" -> throw new AccessDeniedException("해당 결제 대상은 외부 대상 정보 수정이 허용되지 않습니다.");
            default -> throw new AccessDeniedException("지원하지 않는 결제 대상 타입입니다.");
        }
    }

    private void validatePaymentTargetCreationPermission(PaymentTargetType paymentTargetType, Long requestedTargetId, Users user) {
        if (requestedTargetId == null) {
            return;
        }

        String roleCode = user.getRoleCode() != null ? user.getRoleCode().getCode() : null;
        if ("ADMIN".equals(roleCode)) {
            return;
        }

        if ("EVENT_MANAGER".equals(roleCode)) {
            throw new AccessDeniedException("행사 관리자는 결제 대상 정보를 직접 지정할 수 없습니다.");
        }

        String targetCode = paymentTargetType != null
                ? paymentTargetType.getPaymentTargetCode()
                : null;
        Long principalUserId = user.getUserId();

        switch (targetCode) {
            case "RESERVATION" -> validateReservationOwner(requestedTargetId, principalUserId);
            case "BANNER_APPLICATION" -> validateBannerApplicationOwner(requestedTargetId, principalUserId);
            case "BOOTH_APPLICATION" -> validateBoothApplicationOwner(requestedTargetId, principalUserId);
            // BOOTH/AD 생성 경로에는 사용자 소유권을 증명할 매핑이 없어 외부 지정 targetId를 거부한다.
            case "BOOTH", "AD" -> throw new AccessDeniedException("해당 결제 대상은 생성 시 외부 대상 지정이 허용되지 않습니다.");
            default -> throw new AccessDeniedException("지원하지 않는 결제 대상 타입입니다.");
        }
    }

    private void validateReservationOwner(Long reservationId, Long principalUserId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new AccessDeniedException("본인 예약만 결제 대상에 연결할 수 있습니다."));
        Long reservationUserId = reservation.getUser() != null ? reservation.getUser().getUserId() : null;
        if (!Objects.equals(reservationUserId, principalUserId)) {
            throw new AccessDeniedException("본인 예약만 결제 대상에 연결할 수 있습니다.");
        }
    }

    private void validateBannerApplicationOwner(Long bannerApplicationId, Long principalUserId) {
        BannerApplication bannerApplication = bannerApplicationRepository.findById(bannerApplicationId)
                .orElseThrow(() -> new AccessDeniedException("본인 배너 신청만 결제 대상에 연결할 수 있습니다."));
        Long applicantUserId = bannerApplication.getApplicantId() != null
                ? bannerApplication.getApplicantId().getUserId()
                : null;
        if (!Objects.equals(applicantUserId, principalUserId)) {
            throw new AccessDeniedException("본인 배너 신청만 결제 대상에 연결할 수 있습니다.");
        }
    }

    private void validateBoothApplicationOwner(Long boothApplicationId, Long principalUserId) {
        BoothApplication boothApplication = boothApplicationRepository.findById(boothApplicationId)
                .orElseThrow(() -> new AccessDeniedException("본인 부스 신청만 결제 대상에 연결할 수 있습니다."));
        Users boothUser = userRepository.findByEmail(boothApplication.getBoothEmail())
                .orElseThrow(() -> new AccessDeniedException("본인 부스 신청만 결제 대상에 연결할 수 있습니다."));
        if (!Objects.equals(boothUser.getUserId(), principalUserId)) {
            throw new AccessDeniedException("본인 부스 신청만 결제 대상에 연결할 수 있습니다.");
        }
    }

    private boolean isManagedBy(Event event, Long managerUserId) {
        return event != null
                && event.getManager() != null
                && Objects.equals(event.getManager().getUserId(), managerUserId);
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
            case "BOOTH_APPLICATION":
                prefix = "BOOTH";
                break;
            case "BANNER_APPLICATION":
                prefix = "BANNER";
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
        processPaymentCompletionActions(payment, null, null);
    }
    
    /**
     * 결제 완료 후 후속 처리 로직 (scheduleId, ticketId 전달)
     */
    private void processPaymentCompletionActions(Payment payment, Long scheduleId, Long ticketId) {
        String targetType = payment.getPaymentTargetType().getPaymentTargetCode();

        try {
            switch (targetType) {
                case "RESERVATION":
                    processReservationPaymentCompletion(payment, scheduleId, ticketId);
                    break;
                case "BOOTH":
                    processBoothPaymentCompletion(payment);
                    break;
                case "BOOTH_APPLICATION":
                    processBoothApplyPaymentCompletion(payment);
                    break;
                case "BANNER_APPLICATION":
                    processBannerPaymentCompletion(payment);
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
     * 결제 완료 후 예매 생성 (scheduleId, ticketId 직접 전달)
     */
    private Long createReservationAfterPayment(Payment payment, Long scheduleId, Long ticketId) {
        try {
            System.out.println("결제 완료 후 예매 생성 시작 - paymentId: " + payment.getPaymentId());
            
            // 결제 정보에서 예매 생성에 필요한 정보 추출
            // 현재 Payment 엔티티에 scheduleId, ticketId 필드가 없으므로
            // 임시로 더미 데이터를 사용하고, 향후 개선이 필요
            
            Event event = payment.getEvent();
            Users user = payment.getUser();
            
            if (event == null) {
                throw new IllegalArgumentException("이벤트 정보가 없습니다.");
            }
            
            if (user == null) {
                throw new IllegalArgumentException("사용자 정보가 없습니다.");
            }
            

            EventSchedule schedule = null;
            Ticket ticket = null;
            
            // scheduleId가 직접 전달된 경우 해당 스케줄 사용
            if (scheduleId != null) {
                schedule = eventScheduleRepository.findById(scheduleId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스케줄 ID: " + scheduleId));
                System.out.println("🟢 [PaymentService] 전달받은 scheduleId 사용: " + scheduleId);
            } else {
                // 기존 로직: 첫 번째 스케줄 사용
                List<EventSchedule> schedules = eventScheduleRepository.findByEvent_EventId(event.getEventId());
                if (schedules.isEmpty()) {
                    throw new IllegalStateException("이벤트에 스케줄이 없습니다.");
                }
                schedule = schedules.get(0);
                System.out.println("🟡 [PaymentService] 기본 스케줄 사용: " + schedule.getScheduleId());
            }
            
            // ticketId가 직접 전달된 경우 해당 티켓 사용
            if (ticketId != null) {
                ticket = ticketRepository.findById(ticketId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 티켓 ID: " + ticketId));
                System.out.println("🟢 [PaymentService] 전달받은 ticketId 사용: " + ticketId);
            } else {
                // 기존 로직: 첫 번째 티켓 사용
                List<Ticket> tickets = ticketRepository.findTicketsByEventId(event.getEventId());
                if (tickets.isEmpty()) {
                    throw new IllegalStateException("이벤트에 티켓이 없습니다.");
                }
                ticket = tickets.get(0);
                System.out.println("🟡 [PaymentService] 기본 티켓 사용: " + ticket.getTicketId());
            }
            
            // ReservationRequestDto 생성
            ReservationRequestDto reservationRequest = new ReservationRequestDto();
            reservationRequest.setEventId(event.getEventId());
            reservationRequest.setScheduleId(schedule.getScheduleId());
            reservationRequest.setTicketId(ticket.getTicketId());
            reservationRequest.setQuantity(payment.getQuantity());
            reservationRequest.setPrice(payment.getAmount().intValue());
            
            // ReservationService를 사용하여 예약 생성
            Reservation reservation = reservationService.createReservation(
                reservationRequest, 
                user.getUserId(), 
                payment.getPaymentId()
            );
            
            System.out.println("예매 생성 성공 - reservationId: " + reservation.getReservationId() +
                              ", ticketId: " + ticket.getTicketId() + ", quantity: " + payment.getQuantity());

            AttendeeFormSaveRequestDto attendeeFormSaveRequestDto = AttendeeFormSaveRequestDto.builder()
                .reservationId(reservation.getReservationId())
                .totalAllowed(reservation.getQuantity())
                .build();

            // 참석자 저장 및 참석자 링크 폼 생성
            AttendeeFormSaveResponseDto attendeeFormSaveResponseDto =
                attendeeFormAttendeeService.saveAttendeeFormAndAttendee(user.getUserId(), attendeeFormSaveRequestDto);

            System.out.println("참석자 생성 성공 - reservationId: " + reservation.getReservationId() +
                ", 폼 링크 (티켓 매수 1 이상 시: " + attendeeFormSaveResponseDto.getToken() + ", quantity: " + payment.getQuantity());

            return reservation.getReservationId();
            
        } catch (Exception e) {
            System.err.println("예매 생성 실패 - paymentId: " + payment.getPaymentId() +
                              ", error: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("예매 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 예약 결제 완료 처리
     * - 결제 완료 후 예약 생성 (방식 A)
     */
    private void processReservationPaymentCompletion(Payment payment) {
        processReservationPaymentCompletion(payment, null, null);
    }
    
    /**
     * 예약 결제 완료 처리 (scheduleId, ticketId 전달)
     */
    private void processReservationPaymentCompletion(Payment payment, Long scheduleId, Long ticketId) {
        try {
            // 방식 A: 결제 완료 후 예매 생성 (targetId가 null인 경우만)
            if (payment.getTargetId() == null) {
                // targetId가 null이면 결제 후 예매 생성해야 하는 상황
                Long reservationId = createReservationAfterPayment(payment, scheduleId, ticketId);

                System.out.println("🔴 [PaymentService] 예매 생성 완료 - reservationId: " + reservationId);
                
                // payment의 targetId를 실제 예매 ID로 업데이트
                payment.setTargetId(reservationId);
                Payment savedPayment = paymentRepository.save(payment);
                
                System.out.println("🔴 [PaymentService] payment 업데이트 완료 - paymentId: " + savedPayment.getPaymentId() +
                        ", targetId: " + savedPayment.getTargetId());

                System.out.println("결제 후 예매 생성 완료 - paymentId: " + payment.getPaymentId() +
                        ", reservationId: " + reservationId);
                
                // 예약 처리 성공 후 알림 발송
                sendPaymentCompletionNotifications(payment, reservationId);
            } else {
                // targetId가 이미 있는 경우: 기존 예매에 대한 알림만 발송
                System.out.println("기존 예약에 대한 알림 발송 - paymentId: " + payment.getPaymentId() +
                        ", targetId: " + payment.getTargetId());
                sendPaymentCompletionNotifications(payment, payment.getTargetId());
            }

        } catch (Exception e) {
            System.err.println("예약 처리 실패 - paymentId: " + payment.getPaymentId() +
                    ", error: " + e.getMessage());
            throw e; // 예외를 다시 던져서 결제 취소 등의 처리가 가능하도록 함
        }
    }

    /**
     * 부스 신청 결제 완료 처리
     */
    private void processBoothApplyPaymentCompletion(Payment payment) {
        try {
            if (payment.getTargetId() == null) {
                System.err.println("부스 결제 완료 처리 실패 - targetId가 null입니다. paymentId: " + payment.getPaymentId());
                return;
            }

            Long targetId = payment.getTargetId();
            Booth booth = boothRepository.findById(targetId)
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 부스를 찾을 수 없습니다."));
            try {
                BoothApplication boothApplication = boothApplicationRepository
                        .findByBoothEmailOrderByApplyAtDesc(booth.getBoothAdmin().getUser().getEmail())
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "부스 신청 정보를 찾을 수 없습니다."));;
                System.out.println("부스 결제 완료 처리됨 - targetId: " + payment.getTargetId() +
                        ", boothTitle: " + boothApplication.getBoothTitle());
            } catch (Exception e) {
                throw new CustomException(HttpStatus.NOT_FOUND, "부스 신청 정보를 찾을 수 없습니다.");
            }

            // 필요시 여기에 추가 로직 구현 (상태 업데이트, 알림 등)
            // 현재는 BoothPaymentController에서 처리하고 있으므로 로깅만 수행

        } catch (Exception e) {
            System.err.println("부스 결제 완료 처리 중 오류 발생 - paymentId: " + payment.getPaymentId() +
                    ", error: " + e.getMessage());
        }
    }

    /**
     * 부스 결제 완료 처리
     */
    private void processBoothPaymentCompletion(Payment payment) {
        try {
            Long boothId = payment.getTargetId();
            if (boothId == null) {
                System.err.println("부스 결제 완료 처리 실패 - targetId가 null입니다. paymentId: " + payment.getPaymentId());
                return;
            }

            // 부스 신청 정보 조회
            Booth booth = boothRepository.findById(boothId)
                    .orElseThrow(() -> new IllegalArgumentException("부스 신청 정보를 찾을 수 없습니다: " + boothId));;

            System.out.println("부스 결제 완료 처리됨 - targetId: " + payment.getTargetId() +
                    ", boothTitle: " + booth.getBoothTitle());

            // 필요시 여기에 추가 로직 구현 (상태 업데이트, 알림 등)
            // 현재는 BoothPaymentController에서 처리하고 있으므로 로깅만 수행

        } catch (Exception e) {
            System.err.println("부스 결제 완료 처리 중 오류 발생 - paymentId: " + payment.getPaymentId() +
                    ", error: " + e.getMessage());
        }
    }

    /**
     * 배너 결제 완료 처리
     */
    private void processBannerPaymentCompletion(Payment payment) {
        try {
            Long bannerApplicationId = payment.getTargetId();
            if (bannerApplicationId == null) {
                System.err.println("배너 결제 완료 처리 실패 - targetId가 null입니다. paymentId: " + payment.getPaymentId());
                return;
            }

            // 배너 신청 정보 조회
            BannerApplication application = bannerApplicationRepository.findById(bannerApplicationId)
                    .orElseThrow(() -> new IllegalArgumentException("배너 신청 정보를 찾을 수 없습니다: " + bannerApplicationId));

            System.out.println("배너 결제 완료 처리됨 - targetId: " + payment.getTargetId() +
                    ", title: " + application.getTitle());

            // 배너 슬롯 활성화 (향후 승인과 결제를 분리할 때를 대비한 로직)
            // 현재는 BannerPaymentController에서 결제 상태 업데이트만 처리하고 
            // 실제 슬롯 활성화는 markPaid에서 이미 처리됨

        } catch (Exception e) {
            System.err.println("배너 결제 완료 처리 중 오류 발생 - paymentId: " + payment.getPaymentId() +
                    ", error: " + e.getMessage());
        }
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
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("결제 금액이 유효하지 않습니다: " + amount);
        }

        // 최대 결제 금액 제한 (배너 광고 고려하여 1억원으로 상향 조정)
        BigDecimal maxAmount = new BigDecimal("100000000");
        if (amount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("결제 금액이 최대 한도를 초과했습니다 (최대 1억원): " + amount);
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

    /**
     * 결제 완료 알림 발송 (웹 + HTML 이메일 동시)
     */
    public void sendPaymentCompletionNotifications(Payment payment, Long reservationId) {
        try {
            Long userId = payment.getUser().getUserId();
            String eventTitle = payment.getEvent() != null ? payment.getEvent().getTitleKr() : "이벤트";
            BigDecimal amount = payment.getAmount();
            String userName = payment.getUser().getName();

            // 무료 티켓 여부 확인
            boolean isFreeTicket = amount.compareTo(BigDecimal.ZERO) == 0;
            String actionType = isFreeTicket ? "예매" : "결제";

            // 1. 웹 알림 발송 (실시간)
            NotificationRequestDto webNotification = NotificationService.buildWebNotification(
                    userId,
                    isFreeTicket ? "RESERVATION" : "PAYMENT",
                    String.format("%s 완료", actionType),
                    String.format("%s %s가 완료되었습니다! 마이페이지에서 확인해보세요.", eventTitle, actionType),
                    "/mypage/reservation"
            );
            notificationService.createNotification(webNotification);

            // 2. HTML 템플릿 이메일 발송 (새로운 전용 서비스 사용)
            paymentCompletionEmailService.sendPaymentCompletionEmail(payment, reservationId);

            System.out.println(String.format("결제 완료 알림 발송 완료 - userId: %d, paymentId: %d, type: %s",
                    userId, payment.getPaymentId(), actionType));

        } catch (Exception e) {
            // 알림 발송 실패해도 결제는 성공으로 처리
            System.err.println("결제 완료 알림 발송 실패 - paymentId: " + payment.getPaymentId() +
                    ", error: " + e.getMessage());
        }
    }

    /**
     * 결제/예매 완료 이메일 내용 생성
     */
    private String generatePaymentEmailContent(Payment payment, Long reservationId, boolean isFreeTicket) {
        String userName = payment.getUser().getName();
        String eventTitle = payment.getEvent() != null ? payment.getEvent().getTitleKr() : "이벤트";
        String actionType = isFreeTicket ? "예매" : "결제";
        String amountText = isFreeTicket ? "무료" : payment.getAmount().toString() + "원";

        return String.format("""
                        안녕하세요, %s님
                        
                        %s %s가 성공적으로 완료되었습니다.
                        
                        [%s 정보]
                        - 이벤트: %s
                        - %s 금액: %s
                        - %s 일시: %s
                        - 예약 번호: %s
                        - 주문 번호: %s
                        
                        티켓 정보 및 QR 코드는 마이페이지 > 예매 내역에서 확인하실 수 있습니다.
                        
                        행사 당일 QR 코드 또는 예약 번호를 지참해 주시기 바랍니다.
                        
                        문의사항이 있으시면 언제든 고객센터로 연락 주세요.
                        
                        감사합니다.
                        FairPlay 팀
                        """,
                userName,
                eventTitle, actionType,
                actionType,
                eventTitle,
                actionType, amountText,
                actionType, payment.getPaidAt() != null ?
                        payment.getPaidAt().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm")) :
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm")),
                reservationId != null ? reservationId.toString() : "처리중",
                payment.getMerchantUid()
        );
    }

    // 이메일에서 부스 결제 요청 처리 (인증 없이)
    @Transactional
    public PaymentResponseDto savePaymentFromEmail(PaymentRequestDto paymentRequestDto) {
        // 1. 요청 데이터 유효성 검증
        validatePaymentRequest(paymentRequestDto);

        // 2. 결제 타입에 따라 사용자 정보 가져오기
        if (!"BOOTH_APPLICATION".equals(paymentRequestDto.getPaymentTargetType()) && 
            !"BANNER_APPLICATION".equals(paymentRequestDto.getPaymentTargetType())) {
            throw new IllegalArgumentException("이메일 결제는 부스 또는 배너 신청에만 사용 가능합니다.");
        }

        if (paymentRequestDto.getTargetId() == null) {
            throw new IllegalArgumentException("신청 ID가 필요합니다.");
        }

        Users user;
        
        if ("BOOTH_APPLICATION".equals(paymentRequestDto.getPaymentTargetType())) {
            // 3-A. 부스 신청 정보에서 부스 이메일로 사용자 찾기
            BoothApplication boothApplication = boothApplicationRepository.findById(paymentRequestDto.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("부스 신청 정보를 찾을 수 없습니다."));

            user = userRepository.findByEmail(boothApplication.getBoothEmail())
                    .orElseThrow(() -> new IllegalArgumentException("부스 관리자 계정을 찾을 수 없습니다: " + boothApplication.getBoothEmail()));

            // 4-A. 이벤트 정보 설정 (부스 신청에서 가져오기)
            paymentRequestDto.setEventId(boothApplication.getEvent().getEventId());
        } else {
            // 3-B. 배너 신청 정보에서 신청자 ID로 사용자 찾기
            BannerApplication bannerApplication = bannerApplicationRepository.findById(paymentRequestDto.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("배너 신청 정보를 찾을 수 없습니다."));

            user = userRepository.findById(bannerApplication.getApplicantId().getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("배너 신청자 계정을 찾을 수 없습니다: " + bannerApplication.getApplicantId()));

            // 4-B. 이벤트 정보 설정 (배너 신청에서 가져오기)
            paymentRequestDto.setEventId(bannerApplication.getEvent().getEventId());
        }

        // 5. 기존 savePayment 메서드 활용
        return savePayment(paymentRequestDto, user.getUserId());
    }

}
