package com.fairing.fairplay.ticket.service;

import com.fairing.fairplay.ticket.dto.ScheduleTicketRequestDto;
import com.fairing.fairplay.ticket.dto.ScheduleTicketResponseDto;
import com.fairing.fairplay.ticket.entity.EventSchedule;
import com.fairing.fairplay.ticket.entity.EventTicketId;
import com.fairing.fairplay.ticket.entity.ScheduleTicket;
import com.fairing.fairplay.event.repository.EventTicketRepository;
import com.fairing.fairplay.ticket.repository.EventScheduleRepository;
import com.fairing.fairplay.ticket.repository.ScheduleTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleTicketService {

    private final ScheduleTicketRepository scheduleTicketRepository;
    private final EventScheduleRepository eventScheduleRepository;
    private final EventTicketRepository eventTicketRepository;


    // 회차별 티켓 등록
    @Transactional
    public void registerScheduleTicket(Long eventId, Long scheduleId, List<ScheduleTicketRequestDto> ticketList) {

        eventScheduleRepository.findByEvent_EventIdAndScheduleId(eventId, scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 행사에 존재하지 않는 회차입니다. scheduleId: " + scheduleId));
        validateTicketsBelongToEvent(eventId, ticketList);

        // ScheduleTicket 테이블에서 해당 이벤트의 스케쥴에 설정된 티켓 모두 삭제
        scheduleTicketRepository.deleteByEventSchedule_ScheduleId(scheduleId);

        // 신규 입력 받은 티켓 설정 정보 저장
        List<ScheduleTicket> tickets = ScheduleTicket.fromList(ticketList, eventId, scheduleId);

        scheduleTicketRepository.saveAll(tickets);
    }

    // 회차별 티켓 조회
    public List<ScheduleTicketResponseDto> getScheduleTickets(Long eventId, Long scheduleId) {
        return scheduleTicketRepository.findScheduleTickets(eventId, scheduleId);
    }

    private void validateTicketsBelongToEvent(Long eventId, List<ScheduleTicketRequestDto> ticketList) {
        for (ScheduleTicketRequestDto ticket : ticketList) {
            Long ticketId = ticket.getTicketId();
            if (ticketId == null || !eventTicketRepository.existsById(new EventTicketId(ticketId, eventId))) {
                throw new IllegalArgumentException("해당 행사에 존재하지 않는 티켓입니다. ticketId: " + ticketId);
            }
        }
    }
}
