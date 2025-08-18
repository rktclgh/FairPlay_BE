package com.fairing.fairplay.ticket.service;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.ticket.dto.EventScheduleRequestDto;
import com.fairing.fairplay.ticket.dto.EventScheduleResponseDto;
import com.fairing.fairplay.ticket.entity.EventSchedule;
import com.fairing.fairplay.ticket.repository.EventScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventScheduleService {
    
    private final EventScheduleRepository eventScheduleRepository;
    private final EventRepository eventRepository;

    // 회차 등록
    @Transactional
    public EventScheduleResponseDto createSchedule(Long eventId, EventScheduleRequestDto requestDto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행사입니다. eventId: " + eventId));

        // 동일한 날짜에 시간 겹침 확인
        boolean hasTimeConflict = eventScheduleRepository.existsTimeConflict(
                eventId, 
                requestDto.getDate(), 
                requestDto.getStartTime(), 
                requestDto.getEndTime()
        );
        
        if (hasTimeConflict) {
            throw new IllegalArgumentException("해당 날짜에 이미 등록된 시간과 겹칩니다.");
        }

        EventSchedule schedule = EventSchedule.from(requestDto);
        schedule.setEvent(event);
        EventSchedule savedSchedule = eventScheduleRepository.save(schedule);
        return EventScheduleResponseDto.builder()
                .scheduleId(savedSchedule.getScheduleId())
                .date(savedSchedule.getDate())
                .startTime(savedSchedule.getStartTime())
                .endTime(savedSchedule.getEndTime())
                .weekday(savedSchedule.getWeekday())
                .createdAt(savedSchedule.getCreatedAt())
                .build();
    }
    
    // 회차 목록 조회
    @Transactional(readOnly = true)
    public List<EventScheduleResponseDto> getSchedules(Long eventId) {
        List<EventSchedule> schedules = eventScheduleRepository.findAllByEvent_EventId(eventId);
        
        // 회차별 티켓 설정 상태 조회
        List<Map<String, Object>> ticketStatusList = eventScheduleRepository.findScheduleTicketStatus(eventId);
        Map<Long, Boolean> ticketStatusMap = ticketStatusList.stream()
                .collect(Collectors.toMap(
                    row -> (Long) row.get("scheduleId"),
                    row -> (Boolean) row.get("hasActiveTickets")
                ));
        
        // 회차별 판매된 티켓 개수 조회
        List<Map<String, Object>> soldTicketCountList = eventScheduleRepository.findSoldTicketCounts(eventId);
        Map<Long, Long> soldTicketCountMap = soldTicketCountList.stream()
                .collect(Collectors.toMap(
                    row -> (Long) row.get("scheduleId"),
                    row -> (Long) row.get("soldTicketCount")
                ));
        
        return schedules.stream()
                .map(schedule -> EventScheduleResponseDto.from(
                    schedule, 
                    ticketStatusMap.getOrDefault(schedule.getScheduleId(), false),
                    soldTicketCountMap.getOrDefault(schedule.getScheduleId(), 0L)
                ))
                .toList();
    }
    
    // 회차 상세 조회
    @Transactional(readOnly = true)
    public EventScheduleResponseDto getSchedule(Long eventId, Long scheduleId) {
        EventSchedule schedule = eventScheduleRepository.findByEvent_EventIdAndScheduleId(eventId, scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회차입니다. scheduleId: " + scheduleId));
        return EventScheduleResponseDto.from(schedule);
    }

    // 회차 수정
    @Transactional
    public EventScheduleResponseDto updateSchedule(Long eventId, Long scheduleId, EventScheduleRequestDto requestDto) {

        // 동일한 날짜에 시간 겹침 확인 (자기 자신 제외)
        boolean hasTimeConflict = eventScheduleRepository.existsTimeConflictForUpdate(
                eventId,
                scheduleId,
                requestDto.getDate(),
                requestDto.getStartTime(),
                requestDto.getEndTime()
        );

        if (hasTimeConflict) {
            throw new IllegalArgumentException("해당 날짜에 이미 등록된 시간과 겹칩니다.");
        }

        EventSchedule schedule = eventScheduleRepository.findByEvent_EventIdAndScheduleId(eventId, scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회차입니다. scheduleId: " + scheduleId));

        schedule.setDate(requestDto.getDate());
        schedule.setStartTime(requestDto.getStartTime());
        schedule.setEndTime(requestDto.getEndTime());
        // 날짜를 기반으로 요일 자동 계산: 일요일(0) ~ 토요일(6)
        schedule.setWeekday(requestDto.getDate().getDayOfWeek().getValue() % 7);

        return EventScheduleResponseDto.from(schedule);
    }

    // 회차 삭제
    @Transactional
    public void deleteSchedule(Long eventId, Long scheduleId) {
        EventSchedule schedule = eventScheduleRepository.findByEvent_EventIdAndScheduleId(eventId, scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회차입니다. scheduleId: " + scheduleId));
        eventScheduleRepository.delete(schedule);
    }
}
