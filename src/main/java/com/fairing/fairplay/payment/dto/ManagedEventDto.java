package com.fairing.fairplay.payment.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagedEventDto {
    
    private Long eventId;
    private String eventName;
    private String eventStatus;
    private LocalDate startDate;
    private LocalDate endDate;
}