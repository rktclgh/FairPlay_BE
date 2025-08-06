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
    private boolean canceled;

}
