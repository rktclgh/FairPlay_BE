package com.fairing.fairplay.booth.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothExperienceRequestDto {

    private String title; // 체험 제목

    private String description; // 체험 설명

    private LocalDate experienceDate; // 체험 날짜

    private LocalTime startTime; // 운영 시작 시간

    private LocalTime endTime; // 운영 종료 시간

    private Integer durationMinutes; // 체험 소요 시간 (분)

    private Integer maxCapacity; // 최대 동시 체험 인원

    private Boolean allowWaiting; // 대기열 허용 여부

    private Integer maxWaitingCount; // 최대 대기 인원

    private Boolean allowDuplicateReservation; // 중복 예약 허용 여부

    private Boolean isReservationEnabled; // 예약 가능 여부
}