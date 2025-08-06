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
    private TypesEnum types;            // EVENT, BOOTH    
    private LocalDateTime createdAt;    // 생성일시
    
    public static EventScheduleResponseDto from(EventSchedule eventSchedule){
        return EventScheduleResponseDto.builder()
                .scheduleId(eventSchedule.getScheduleId())
                .date(eventSchedule.getDate())
                .startTime(eventSchedule.getStartTime())
                .endTime(eventSchedule.getEndTime())
                .weekday(eventSchedule.getWeekday())
                .types(eventSchedule.getTypes())
                .createdAt(eventSchedule.getCreatedAt())
                .build();
    }
}
