package com.fairing.fairplay.reservation.service;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.notification.dto.NotificationRequestDto;
import com.fairing.fairplay.notification.service.NotificationService;
import com.fairing.fairplay.reservation.dto.ReservationRequestDto;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.entity.ReservationLog;
import com.fairing.fairplay.reservation.entity.ReservationStatusCode;
import com.fairing.fairplay.reservation.entity.ReservationStatusCodeEnum;
import com.fairing.fairplay.reservation.repository.ReservationLogRepository;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ReservationLogRepository reservationLogRepository;
    private final NotificationService notificationService; // NotificationService 주입
    /*
    private final EventScheduleRepository eventScheduleRepository;
    private final TicketRepository ticketRepository;
    private final ScheduleTicketRepository scheduleTicketRepository;
     */

    // 예약 신청
    @Transactional
    public Reservation createReservation(ReservationRequestDto requestDto, Long userId) {

        Event event = eventRepository.findById(requestDto.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 EVENT ID: " + requestDto.getEventId()));

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자 ID: " + userId));

        // 일정 등록 여부 확인 (Null 허용)
        if(requestDto.getScheduleId() != null){

        }

        // 티켓 오픈 확인, 티켓 판매 기간 확인

        /*

        EventSchedule schedule = eventScheduleRepository.getReferenceById(requestDto.getScheduleId());
        Ticket ticket = ticketRepository.getReferenceById(requestDto.getTicketId());

        Reservation reservationParam =  new Reservation(event, schedule, ticket, user, requestDto.getQuantity(), requestDto.getPrice());
        Reservation reservation = reservationRepository.save(reservationParam);

        // --- 알림 생성 로직 추가 ---
        NotificationRequestDto notificationDto = NotificationRequestDto.builder()
                .userId(userId)
                .typeCode("RESERVATION")
                .methodCode("WEB")
                .title(event.getName() + " 예약 완료!")
                .message(user.getName() + " 님, " + event.getName() + " 박람회 예약이 성공적으로 완료되었습니다.")
                .url("https://fair-play.ink/event/" + event.getEventId())
                .build();
        
        notificationService.createNotification(notificationDto);
        // --------------------------

        return reservation;
         */

        return null;
    }

    // 예약 상세 조회
    @Transactional(readOnly = true)
    public Reservation getReservationById(Long reservationId) {

        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약 ID: " + reservationId));
    }

    // 특정 행사의 전체 예약 조회
    @Transactional(readOnly = true)
    public List<Reservation> getReservationsByEvent(Long eventId) {

        return reservationRepository.findByEvent_EventId(eventId);
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

        // 취소된 티켓 수량만큼 재고 증가
        /*
        ScheduleTicketId scheduleTicketId = new ScheduleTicketId(reservation.getTicket().getTicketId(), reservation.getSchedule().getScheduleId());
        ScheduleTicket scheduleTicket = scheduleTicketRepository.findById(scheduleTicketId);
        if (scheduleTicket != null) {
            scheduleTicket.setRemainingStock(
                scheduleTicket.getRemainingStock() + reservation.getQuantity()
            );
        }
        */

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
}
