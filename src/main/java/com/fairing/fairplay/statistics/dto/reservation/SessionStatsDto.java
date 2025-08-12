package com.fairing.fairplay.statistics.dto.reservation;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
public class SessionStatsDto {
    private Long sessionId;
    private LocalDate stat_date;
    private LocalTime startTime;
    private String sessionName;
    private Integer reservations;
    private Integer checkins;
    private Integer cancellation;
    private Integer noShows;



}
