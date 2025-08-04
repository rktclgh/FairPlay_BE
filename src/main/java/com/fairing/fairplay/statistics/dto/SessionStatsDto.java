package com.fairing.fairplay.statistics.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SessionStatsDto {
    private Long sessionId;
    private String sessionName;
    private Integer reservations;
}
