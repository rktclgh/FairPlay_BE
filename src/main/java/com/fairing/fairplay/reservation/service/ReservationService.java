package com.fairing.fairplay.reservation.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.repository.AttendeeRepository;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.notification.dto.NotificationRequestDto;
import com.fairing.fairplay.notification.service.NotificationService;
import com.fairing.fairplay.payment.dto.PaymentRequestDto;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.entity.PaymentStatusCode;
import com.fairing.fairplay.payment.repository.PaymentRepository;
import com.fairing.fairplay.payment.repository.PaymentStatusCodeRepository;
import com.fairing.fairplay.reservation.dto.ReservationAttendeeDto;
import com.fairing.fairplay.reservation.dto.ReservationRequestDto;
import com.fairing.fairplay.reservation.dto.ReservationResponseDto;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.entity.ReservationLog;
import com.fairing.fairplay.reservation.entity.ReservationStatusCode;
import com.fairing.fairplay.reservation.entity.ReservationStatusCodeEnum;
import com.fairing.fairplay.reservation.repository.ReservationLogRepository;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.ticket.entity.EventSchedule;
import com.fairing.fairplay.ticket.entity.ScheduleTicket;
import com.fairing.fairplay.ticket.entity.ScheduleTicketId;
import com.fairing.fairplay.ticket.entity.Ticket;
import com.fairing.fairplay.ticket.repository.EventScheduleRepository;
import com.fairing.fairplay.ticket.repository.ScheduleTicketRepository;
import com.fairing.fairplay.ticket.repository.TicketRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final AttendeeRepository attendeeRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ReservationLogRepository reservationLogRepository;
    private final NotificationService notificationService;
    private final EventScheduleRepository eventScheduleRepository;
    private final TicketRepository ticketRepository;
    private final ScheduleTicketRepository scheduleTicketRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentStatusCodeRepository paymentStatusCodeRepository;

    // 예약 신청 (결제 데이터 생성 이후 마지막에 결제 완료 상태로 저장)
    @Transactional
    public Reservation createReservation(ReservationRequestDto requestDto, Long userId, Long paymentId) {
        
        System.out.println("🔵 [ReservationService] createReservation 호출 - userId: " + userId + 
                ", paymentId: " + paymentId + ", eventId: " + requestDto.getEventId() + 
                ", scheduleId: " + requestDto.getScheduleId() + ", ticketId: " + requestDto.getTicketId());

        Event event = eventRepository.findById(requestDto.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 EVENT ID: " + requestDto.getEventId()));

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자 ID: " + userId));

        // 일정 정보 확인 (null일 수 있음)
        EventSchedule schedule = null;
        if (requestDto.getScheduleId() != null) {
            schedule = eventScheduleRepository.findById(requestDto.getScheduleId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일정 ID: " + requestDto.getScheduleId()));
        }

        // 티켓 정보 확인
        Ticket ticket = ticketRepository.findById(requestDto.getTicketId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 티켓 ID: " + requestDto.getTicketId()));

        // 티켓 재고 및 판매 기간 확인 (일정이 있는 경우만)
        if (schedule != null) {
            ScheduleTicketId scheduleTicketId = new ScheduleTicketId(ticket.getTicketId(), schedule.getScheduleId());
            
            // 🔒 동시성 문제 해결: 비관적 락으로 재고 조회
            ScheduleTicket scheduleTicket = scheduleTicketRepository.findByIdWithPessimisticLock(scheduleTicketId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 일정에 대한 티켓이 존재하지 않습니다."));

            // 판매 기간 확인
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(scheduleTicket.getSalesStartAt()) || now.isAfter(scheduleTicket.getSalesEndAt())) {
                throw new IllegalStateException("티켓 판매 기간이 아닙니다.");
            }

            // 🚀 원자적 재고 차감 (더 안전한 방법)
            int updatedRows = scheduleTicketRepository.decreaseStockIfAvailable(
                    ticket.getTicketId(), 
                    schedule.getScheduleId(), 
                    requestDto.getQuantity()
            );
            
            if (updatedRows == 0) {
                // 재고 부족으로 업데이트 실패
                throw new IllegalStateException("재고가 부족합니다. 다시 시도해 주세요.");
            }
        }

        // 예약 상태를 CONFIRMED로 설정 (완료 상태)
        ReservationStatusCode confirmedStatus = new ReservationStatusCode(ReservationStatusCodeEnum.CONFIRMED.getId());
        
        // 예약 생성
        Reservation reservation = new Reservation(event, schedule, ticket, user, requestDto.getQuantity(), requestDto.getPrice());
        reservation.setReservationStatusCode(confirmedStatus);
        Reservation savedReservation = reservationRepository.save(reservation);
        
        System.out.println("🔵 [ReservationService] 예매 생성 완료 - reservationId: " + savedReservation.getReservationId() + 
                ", eventId: " + savedReservation.getEvent().getEventId() + 
                ", scheduleId: " + (savedReservation.getSchedule() != null ? savedReservation.getSchedule().getScheduleId() : null));

        Payment payment = paymentRepository.findById(paymentId).orElseThrow();
        
        // 결제 완료 상태로 업데이트
        PaymentStatusCode completedStatus = paymentStatusCodeRepository.findByCode("COMPLETED")
                .orElseThrow(() -> new IllegalStateException("COMPLETED 상태 코드를 찾을 수 없습니다."));
        payment.setPaymentStatusCode(completedStatus);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 알림은 별도 서비스에서 처리 (순환참조 방지)

        return savedReservation;
    }

    @Transactional
    public ReservationResponseDto createReservationResponse(ReservationRequestDto requestDto, Long userId, Long paymentId) {
        Reservation reservation = createReservation(requestDto, userId, paymentId);
        return toResponseDto(reservation);
    }

    // 결제가 있는 예약 생성 (결제 우선 플로우)
    @Transactional
    private Reservation createReservationWithPayment(ReservationRequestDto requestDto, Long userId, 
                                                   Event event, EventSchedule schedule, Ticket ticket, Users user) {
        try {
            // 1단계: 결제 요청 정보 먼저 저장 (PENDING 상태)
            PaymentRequestDto paymentRequest = new PaymentRequestDto();
            paymentRequest.setEventId(requestDto.getEventId());
            paymentRequest.setAmount(BigDecimal.valueOf(requestDto.getPrice()));
            paymentRequest.setQuantity(requestDto.getQuantity());
            paymentRequest.setPrice(BigDecimal.valueOf(requestDto.getPrice() / requestDto.getQuantity())); // 단가
            paymentRequest.setPaymentTargetType("RESERVATION");
            paymentRequest.setMerchantUid(requestDto.getPaymentData().getMerchant_uid());
            
            // 임시로 targetId를 0으로 설정 (예약 생성 후 업데이트 예정)
            paymentRequest.setTargetId(0L);
            // PaymentService 호출 제거 (순환참조 방지) - 이 메서드는 사용하지 않음
            
            // 2단계: 예약 생성 (초기 상태는 PENDING)
            ReservationStatusCode pendingStatus = new ReservationStatusCode(ReservationStatusCodeEnum.PENDING.getId());
            
            Reservation reservationParam = new Reservation(event, schedule, ticket, user, requestDto.getQuantity(), requestDto.getPrice());
            reservationParam.setReservationStatusCode(pendingStatus);
            reservationParam.setCreatedAt(LocalDateTime.now());
            reservationParam.setUpdatedAt(LocalDateTime.now());
            
            Reservation reservation = reservationRepository.save(reservationParam);
            
            // 3단계: 결제의 targetId를 실제 예약 ID로 업데이트
            updatePaymentTargetId(requestDto.getPaymentData().getMerchant_uid(), reservation.getReservationId());
            
            // 4단계: PG 결제 완료 정보로 상태 업데이트
            paymentRequest.setTargetId(reservation.getReservationId());
            paymentRequest.setImpUid(requestDto.getPaymentData().getImp_uid());
            // PaymentService 호출 제거 (순환참조 방지) - 이 메서드는 사용하지 않음
            
            // 5단계: 예약 상태를 CONFIRMED로 변경
            ReservationStatusCode confirmedStatus = new ReservationStatusCode(ReservationStatusCodeEnum.CONFIRMED.getId());
            reservation.setReservationStatusCode(confirmedStatus);
            reservationRepository.save(reservation);
            
            // 예약 상태 로깅 (CONFIRMED)
            createReservationLog(reservation, ReservationStatusCodeEnum.CONFIRMED, userId);
            
            // 알림 생성
            createReservationNotification(reservation, user, event);

            return reservation;
            
        } catch (Exception e) {
            // 결제 처리 실패 시 예외를 다시 던져서 전체 트랜잭션 롤백
            throw new RuntimeException("결제 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    // 결제가 없는 예약 생성 (무료 이벤트)
    @Transactional  
    private Reservation createReservationWithoutPayment(ReservationRequestDto requestDto, Long userId,
                                                       Event event, EventSchedule schedule, Ticket ticket, Users user) {
        // 예약 생성 (초기 상태는 PENDING)
        ReservationStatusCode pendingStatus = new ReservationStatusCode(ReservationStatusCodeEnum.PENDING.getId());
        
        Reservation reservationParam = new Reservation(event, schedule, ticket, user, requestDto.getQuantity(), requestDto.getPrice());
        reservationParam.setReservationStatusCode(pendingStatus);
        reservationParam.setCreatedAt(LocalDateTime.now());
        reservationParam.setUpdatedAt(LocalDateTime.now());
        
        Reservation reservation = reservationRepository.save(reservationParam);

        // 결제 정보가 없는 경우 PENDING 상태로 로깅
        createReservationLog(reservation, ReservationStatusCodeEnum.PENDING, userId);
        
        // 알림 생성
        createReservationNotification(reservation, user, event);
        
        return reservation;
    }

    // 결제의 targetId 업데이트 (헬퍼 메서드) - 순환참조 방지로 직접 구현
    private void updatePaymentTargetId(String merchantUid, Long reservationId) {
        Payment payment = paymentRepository.findByMerchantUid(merchantUid)
            .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: " + merchantUid));
        payment.setTargetId(reservationId);
        paymentRepository.save(payment);
    }

    // 예약 알림 생성 (헬퍼 메서드)
    private void createReservationNotification(Reservation reservation, Users user, Event event) {
        NotificationRequestDto notificationDto = NotificationRequestDto.builder()
                .userId(user.getUserId())
                .typeCode("RESERVATION")
                .methodCode("WEB")
                .title(event.getTitleKr() + " 예약 완료!")
                .message(user.getName() + " 님, " + event.getTitleKr() + " 박람회 예약이 성공적으로 완료되었습니다.")
                .url("https://fair-play.ink/event/" + event.getEventId())
                .build();
        
        notificationService.createNotification(notificationDto);
    }

    // 예약 수정
    @Transactional
    public Reservation updateReservation(ReservationRequestDto requestDto, Long userId) {
        // 기존 예약 조회
        Reservation existingReservation = reservationRepository.findById(requestDto.getReservationId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약 ID: " + requestDto.getReservationId()));

        // 예약자 본인 확인
        if (!existingReservation.getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("예약 수정 권한이 없습니다.");
        }

        // 취소된 예약인지 확인
        if (existingReservation.getReservationStatusCode().getId() == ReservationStatusCodeEnum.CANCELLED.getId()) {
            throw new IllegalStateException("취소된 예약은 수정할 수 없습니다.");
        }

        // 행사 시작 전인지 확인
        LocalDate today = LocalDate.now();
        if (existingReservation.getSchedule() != null &&
                existingReservation.getSchedule().getDate().isBefore(today)) {
            throw new IllegalStateException("행사가 이미 시작되어 수정이 불가능합니다.");
        }

        // 새로운 티켓 정보 확인 (티켓 변경이 있는 경우)
        Ticket newTicket = null;
        if (requestDto.getTicketId() != null && !requestDto.getTicketId().equals(existingReservation.getTicket().getTicketId())) {
            newTicket = ticketRepository.findById(requestDto.getTicketId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 티켓 ID: " + requestDto.getTicketId()));
        }

        // 수량 변경이나 티켓 변경이 있는 경우 재고 처리
        if (existingReservation.getSchedule() != null) {
            // 🔒 기존 예약 재고 복원 (원자적 처리)
            scheduleTicketRepository.increaseStock(
                    existingReservation.getTicket().getTicketId(),
                    existingReservation.getSchedule().getScheduleId(),
                    existingReservation.getQuantity()
            );

            // 새로운 예약에 대한 재고 확인 및 차감
            Ticket targetTicket = newTicket != null ? newTicket : existingReservation.getTicket();
            ScheduleTicketId newScheduleTicketId = new ScheduleTicketId(
                    targetTicket.getTicketId(),
                    existingReservation.getSchedule().getScheduleId()
            );
            
            // 판매 기간 확인을 위해 락으로 조회
            ScheduleTicket newScheduleTicket = scheduleTicketRepository.findByIdWithPessimisticLock(newScheduleTicketId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 일정에 대한 티켓이 존재하지 않습니다."));

            // 판매 기간 확인
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(newScheduleTicket.getSalesStartAt()) || now.isAfter(newScheduleTicket.getSalesEndAt())) {
                throw new IllegalStateException("티켓 판매 기간이 아닙니다.");
            }

            // 🚀 원자적 재고 차감
            int updatedRows = scheduleTicketRepository.decreaseStockIfAvailable(
                    targetTicket.getTicketId(),
                    existingReservation.getSchedule().getScheduleId(),
                    requestDto.getQuantity()
            );
            
            if (updatedRows == 0) {
                // 재고 부족으로 실패 - 이미 복원한 재고를 다시 차감
                scheduleTicketRepository.decreaseStockIfAvailable(
                        existingReservation.getTicket().getTicketId(),
                        existingReservation.getSchedule().getScheduleId(),
                        existingReservation.getQuantity()
                );
                throw new IllegalStateException("재고가 부족합니다. 다시 시도해 주세요.");
            }
        }

        // 예약 정보 업데이트
        if (newTicket != null) {
            existingReservation.setTicket(newTicket);
        }
        existingReservation.setQuantity(requestDto.getQuantity());
        existingReservation.setPrice(requestDto.getPrice());
        existingReservation.setUpdatedAt(LocalDateTime.now());

        Reservation updatedReservation = reservationRepository.save(existingReservation);

        // 예약 수정 로깅
        createReservationLog(updatedReservation, ReservationStatusCodeEnum.fromId(updatedReservation.getReservationStatusCode().getId()), userId);

        return updatedReservation;
    }

    @Transactional
    public ReservationResponseDto updateReservationResponse(ReservationRequestDto requestDto, Long userId) {
        Reservation reservation = updateReservation(requestDto, userId);
        return toResponseDto(reservation);
    }

    // 예약 상세 조회
    @Transactional(readOnly = true)
    public Reservation getReservationById(Long reservationId) {

        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약 ID: " + reservationId));
    }

    @Transactional(readOnly = true)
    public ReservationResponseDto getReservationResponseById(Long reservationId) {
        Reservation reservation = reservationRepository.findByIdForResponse(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약 ID: " + reservationId));
        return toResponseDto(reservation);
    }

    // 특정 행사의 전체 예약 조회
    @Transactional(readOnly = true)
    public List<Reservation> getReservationsByEvent(Long eventId) {

        return reservationRepository.findByEvent_EventId(eventId);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponseDto> getReservationResponsesByEvent(Long eventId) {
        return reservationRepository.findByEventIdForResponse(eventId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    // 예약 취소
    @Transactional
    public void cancelReservation(Long reservationId, Long userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약 ID: " + reservationId));

        // 예약자 본인 확인
        if (!reservation.getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("예약 취소 권한이 없습니다.");
        }

        // 이미 취소된 예약인지 확인
        if (reservation.getReservationStatusCode().getId() == ReservationStatusCodeEnum.CANCELLED.getId()) {
            throw new IllegalStateException("이미 취소된 예약입니다.");
        }

        // 예약 취소 가능 기간 확인
        LocalDate today = LocalDate.now();
        if (reservation.getSchedule() != null &&
                reservation.getSchedule().getDate().isBefore(today)) {
            throw new IllegalStateException("행사가 이미 시작되어 취소가 불가능합니다.");
        }

        // 예약 상태를 취소로 변경
        ReservationStatusCode cancelledStatus = new ReservationStatusCode(ReservationStatusCodeEnum.CANCELLED.getId());
        reservation.setReservationStatusCode(cancelledStatus);

        // 🔒 취소된 티켓 수량만큼 재고 증가 (일정이 있는 경우만)
        if (reservation.getSchedule() != null) {
            scheduleTicketRepository.increaseStock(
                    reservation.getTicket().getTicketId(),
                    reservation.getSchedule().getScheduleId(),
                    reservation.getQuantity()
            );
        }

        reservationRepository.save(reservation);

        // 예약 상태 변경 로깅
        createReservationLog(reservation, ReservationStatusCodeEnum.CANCELLED, userId);
    }

    // 예약 상태 변경 로깅
    private void createReservationLog(Reservation reservation, ReservationStatusCodeEnum changedStatusCode, Long changedByUserId) {

        ReservationStatusCode reservationStatusCode = new ReservationStatusCode(changedStatusCode.getId());
        Users changedBy = userRepository.getReferenceById(changedByUserId);

        ReservationLog log = new ReservationLog(reservation, reservationStatusCode, changedBy);

        reservationLogRepository.save(log);
    }

    // 나의 예약 목록 조회
    @Transactional(readOnly = true)
    public List<Reservation> getMyReservations(Long userId) {
        return reservationRepository.findByUser_userId(userId);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponseDto> getMyReservationResponses(Long userId) {
        List<ReservationResponseDto> responses = reservationRepository.findByUserIdForResponse(userId).stream()
                .map(this::toResponseDto)
                .toList();
        populateReservationPayments(responses);
        return responses;
    }

    private ReservationResponseDto toResponseDto(Reservation reservation) {
        return ReservationResponseDto.from(reservation);
    }

    private void populateReservationPayments(List<ReservationResponseDto> responses) {
        List<Long> reservationIds = responses.stream()
                .map(ReservationResponseDto::getReservationId)
                .toList();
        if (reservationIds.isEmpty()) {
            return;
        }

        Map<Long, ReservationResponseDto> responseByReservationId = responses.stream()
                .collect(Collectors.toMap(ReservationResponseDto::getReservationId, Function.identity()));

        List<Payment> payments = paymentRepository.findByTargetIdsAndPaymentTargetTypeWithCodes(
                reservationIds, "RESERVATION");
        for (Payment payment : payments) {
            ReservationResponseDto dto = responseByReservationId.get(payment.getTargetId());
            if (dto == null) {
                continue;
            }
            dto.setPaymentId(payment.getPaymentId());
            dto.setMerchantUid(payment.getMerchantUid());
            dto.setImpUid(payment.getImpUid());
            dto.setPaymentAmount(payment.getAmount());
            dto.setPaymentStatus(payment.getPaymentStatusCode().getName());
            dto.setPaymentMethod(payment.getPaymentTypeCode().getName());
            dto.setPaidAt(payment.getPaidAt());
        }
    }

    // 참가자 명단 조회 (행사 관리자용) - 페이지네이션 지원
    @Transactional(readOnly = true)
    public Page<ReservationAttendeeDto> getReservationAttendees(
            Long eventId, String status, String name, String phone, Long reservationId, Pageable pageable) {
        
        // AttendeeRepository에서 페이지네이션과 필터링을 지원하는 메서드 호출
        Page<Attendee> attendeePage = attendeeRepository.findAttendeesWithFilters(
                eventId, status, name, phone, reservationId, pageable);
        
        // Attendee를 ReservationAttendeeDto로 변환
        return attendeePage.map(ReservationAttendeeDto::from);
    }

    // 참가자 명단 조회 (엑셀 다운로드용)
    @Transactional(readOnly = true)
    public List<ReservationAttendeeDto> getReservationAttendees(Long eventId, String status) {
        List<Attendee> attendees = attendeeRepository.findAttendeesByEventId(eventId, status);
        
        return attendees.stream()
                .map(ReservationAttendeeDto::from)
                .toList();
    }

    // 참가자 명단 엑셀 파일 생성
    @Transactional(readOnly = true)
    public byte[] generateAttendeesExcel(Long eventId, String status) throws IOException {
        List<ReservationAttendeeDto> attendees = getReservationAttendees(eventId, status);
        
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("참가자 명단");
            
            // 헤더 스타일 생성
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // 헤더 생성
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "예약번호", "참가자명", "이메일", "전화번호", "행사명", "일정", 
                "티켓명", "개별가격", "예약상태", "등록일시", "수정일시", "취소여부", "취소일시"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 데이터 행 생성
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            int rowNum = 1;
            
            for (ReservationAttendeeDto attendee : attendees) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(attendee.getReservationId());
                row.createCell(1).setCellValue(attendee.getUserName());
                row.createCell(2).setCellValue(attendee.getUserEmail());
                row.createCell(3).setCellValue(attendee.getUserPhone());
                row.createCell(4).setCellValue(attendee.getEventName());
                row.createCell(5).setCellValue(attendee.getScheduleName());
                row.createCell(6).setCellValue(attendee.getTicketName());
                row.createCell(7).setCellValue(attendee.getPrice()); // 개별가격으로 변경 (quantity 제거)
                row.createCell(8).setCellValue(attendee.getReservationStatus());
                row.createCell(9).setCellValue(attendee.getCreatedAt() != null ? attendee.getCreatedAt().format(formatter) : "");
                row.createCell(10).setCellValue(attendee.getUpdatedAt() != null ? attendee.getUpdatedAt().format(formatter) : "");
                row.createCell(11).setCellValue(attendee.isCanceled() ? "예" : "아니오");
                row.createCell(12).setCellValue(attendee.getCanceledAt() != null ? attendee.getCanceledAt().format(formatter) : "");
            }
            
            // 컬럼 너비 자동 조정
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
