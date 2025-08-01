package com.fairing.fairplay.reservation.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReservationRequestDto {

    private Long eventId;
    private Long scheduleId;
    private Long ticketId;
    private Long reservationId;

    private int quantity;
    private int price;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private boolean canceled;
    private LocalDateTime canceled_at;

}
