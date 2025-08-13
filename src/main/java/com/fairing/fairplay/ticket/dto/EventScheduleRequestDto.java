package com.fairing.fairplay.ticket.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventScheduleRequestDto {  // 회차 등록
    
    private LocalDate date;             // 행사 일자
    private LocalTime startTime;        // 회차 시작 시각
    private LocalTime endTime;          // 회차 종료 시각
    private Integer weekday;            // 0 (일) ~ 6 (토)
}
