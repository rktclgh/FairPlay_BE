package com.fairing.fairplay.statistics.dto.reservation;

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
    private LocalDate statDate;
    private LocalTime startTime;
    private String sessionName;
    private Integer reservations;
    private Integer checkins;
    private Integer cancellations;
    private Integer noShows;

}
