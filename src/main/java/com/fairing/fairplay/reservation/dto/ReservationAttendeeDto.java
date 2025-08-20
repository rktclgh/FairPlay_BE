package com.fairing.fairplay.reservation.dto;

import com.fairing.fairplay.attendee.entity.Attendee;
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

    public static ReservationAttendeeDto from(Attendee attendee) {
        ReservationAttendeeDto dto = new ReservationAttendeeDto();
        Reservation reservation = attendee.getReservation();
        
        dto.reservationId = reservation.getReservationId();
        // 참가자 정보 사용 (attendee 테이블에서)
        dto.userName = attendee.getName() != null ? attendee.getName() : reservation.getUser().getName();
        dto.userEmail = attendee.getEmail() != null ? attendee.getEmail() : reservation.getUser().getEmail();
        dto.userPhone = attendee.getPhone() != null ? attendee.getPhone() : reservation.getUser().getPhone();
        // 예약 정보
        dto.eventName = reservation.getEvent().getTitleKr();
        dto.scheduleName = reservation.getSchedule() != null ? 
                reservation.getSchedule().getDate() + " " + 
                reservation.getSchedule().getStartTime() + "-" + 
                reservation.getSchedule().getEndTime() : "일정 미정";
        dto.ticketName = reservation.getTicket().getName();
        dto.quantity = 1; // 참가자는 개별 단위이므로 항상 1
        dto.price = reservation.getTicket().getPrice(); // 개별 티켓 가격
        dto.reservationStatus = reservation.getReservationStatusCode().getName();
        dto.createdAt = attendee.getCreatedAt();
        dto.updatedAt = reservation.getUpdatedAt();
        dto.canceled = reservation.isCanceled();
        dto.canceledAt = reservation.getCanceled_at();
        return dto;
    }
}