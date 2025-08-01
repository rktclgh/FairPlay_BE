package com.fairing.fairplay.reservation.service;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.reservation.dto.ReservationRequestDto;
import com.fairing.fairplay.reservation.dto.ReservationResponseDto;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.ticket.entity.EventSchedule;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    /*
    private final EventScheduleRepository eventScheduleRepository;
    private final TicketRepository ticketRepository;
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

        return reservation;
         */

        return null;
    }

    // 예약 상세 조회
    public Reservation getReservationById(ReservationRequestDto requestDto) {

        return reservationRepository.findById(requestDto.getReservationId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약 ID: " + requestDto.getReservationId()));
    }

    // 특정 행사의 전체 예약 조회
    public List<Reservation> getReservationsByEvent(Long eventId) {

        return reservationRepository.findByEvent_EventId(eventId);
    }
}
