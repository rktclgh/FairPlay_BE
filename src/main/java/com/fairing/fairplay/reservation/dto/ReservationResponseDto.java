package com.fairing.fairplay.reservation.dto;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.ticket.entity.EventSchedule;
import com.fairing.fairplay.ticket.entity.Ticket;
import com.fairing.fairplay.user.entity.Users;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ReservationResponseDto {

    // 박람회(행사) 정보
    private Event event;
    // 회차 정보 (일정)
    private EventSchedule schedule;
    // 티켓 정보
    private Ticket ticket;
    // 예약자 정보
    private Users user;

    // 예약 정보
    private int quantity;
    private int price;
    private LocalDateTime created_at;
    private LocalDateTime updated_at;
    private boolean canceled;
    private LocalDateTime canceled_at;

    public ReservationResponseDto(Event event, EventSchedule schedule, Ticket ticket, Users user, int quantity, int price) {
        this.event = event;
        this.schedule = schedule;
        this.ticket = ticket;
        this.user = user;
        this.quantity = quantity;
        this.price = price;
    }
}
