package com.fairing.fairplay.reservation.dto;

import com.fairing.fairplay.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationAttendeeDto {
    
    private Long reservationId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String eventName;
    private String scheduleName;
    private String ticketName;
    private int quantity;
    private int price;
    private String reservationStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean canceled;
    private LocalDateTime canceledAt;

    public static ReservationAttendeeDto from(Reservation reservation) {
        ReservationAttendeeDto dto = new ReservationAttendeeDto();
        dto.reservationId = reservation.getReservationId();
        dto.userName = reservation.getUser().getName();
        dto.userEmail = reservation.getUser().getEmail();
        dto.userPhone = reservation.getUser().getPhone();
        dto.eventName = reservation.getEvent().getTitleKr();
        dto.scheduleName = reservation.getSchedule() != null ? 
                reservation.getSchedule().getDate() + " " + 
                reservation.getSchedule().getStartTime() + "-" + 
                reservation.getSchedule().getEndTime() : "일정 미정";
        dto.ticketName = reservation.getTicket().getName();
        dto.quantity = reservation.getQuantity();
        dto.price = reservation.getPrice();
        dto.reservationStatus = reservation.getReservationStatusCode().getName();
        dto.createdAt = reservation.getCreatedAt();
        dto.updatedAt = reservation.getUpdatedAt();
        dto.canceled = reservation.isCanceled();
        dto.canceledAt = reservation.getCanceled_at();
        return dto;
    }
}