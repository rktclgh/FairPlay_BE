package com.fairing.fairplay.ticket.dto;

import com.fairing.fairplay.ticket.entity.EventSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fairing.fairplay.ticket.entity.TypesEnum;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventScheduleResponseDto { // 회차 목록

    private Long scheduleId;            // 스케쥴 ID
    private LocalDate date;             // 행사 일자
    private LocalTime startTime;        // 회차 시작 시각
    private LocalTime endTime;          // 회차 종료 시각
    private Integer weekday;            // 0 (일) ~ 6 (토)
    private LocalDateTime createdAt;    // 생성일시
    private Boolean hasActiveTickets;   // 활성화된 티켓 존재 여부
    private Long soldTicketCount;       // 판매된 티켓 개수
    
    public static EventScheduleResponseDto from(EventSchedule eventSchedule){
        return EventScheduleResponseDto.builder()
                .scheduleId(eventSchedule.getScheduleId())
                .date(eventSchedule.getDate())
                .startTime(eventSchedule.getStartTime())
                .endTime(eventSchedule.getEndTime())
                .weekday(eventSchedule.getWeekday())
                .createdAt(eventSchedule.getCreatedAt())
                .hasActiveTickets(false) // 기본값, 서비스에서 별도 설정
                .soldTicketCount(0L) // 기본값, 서비스에서 별도 설정
                .build();
    }
    
    public static EventScheduleResponseDto from(EventSchedule eventSchedule, Boolean hasActiveTickets, Long soldTicketCount){
        return EventScheduleResponseDto.builder()
                .scheduleId(eventSchedule.getScheduleId())
                .date(eventSchedule.getDate())
                .startTime(eventSchedule.getStartTime())
                .endTime(eventSchedule.getEndTime())
                .weekday(eventSchedule.getWeekday())
                .createdAt(eventSchedule.getCreatedAt())
                .hasActiveTickets(hasActiveTickets)
                .soldTicketCount(soldTicketCount)
                .build();
    }
}
