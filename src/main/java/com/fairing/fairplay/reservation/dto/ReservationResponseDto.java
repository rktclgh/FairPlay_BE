package com.fairing.fairplay.reservation.dto;

import com.fairing.fairplay.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponseDto {

    private Long reservationId;
    
    // 박람회(행사) 정보
    private Long eventId;
    private String eventName;
    private String eventDescription;
    
    // 회차 정보 (일정)
    private Long scheduleId;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    
    // 티켓 정보
    private Long ticketId;
    private String ticketName;
    private String ticketDescription;
    private Integer ticketPrice;
    
    // 예약자 정보
    private Long userId;
    private String userName;
    private String userEmail;

    // 예약 정보
    private int quantity;
    private int price;
    private String reservationStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean canceled;
    private LocalDateTime canceledAt;
    
    // 결제 정보
    private Long paymentId;
    private String merchantUid;
    private String impUid;
    private BigDecimal paymentAmount;
    private String paymentStatus;
    private LocalDateTime paidAt;

    public static ReservationResponseDto from(Reservation reservation) {
        ReservationResponseDto dto = new ReservationResponseDto();
        
        dto.reservationId = reservation.getReservationId();
        
        // 행사 정보
        dto.eventId = reservation.getEvent().getEventId();
        dto.eventName = reservation.getEvent().getTitleKr();
        dto.eventDescription = reservation.getEvent().getTitleEng();
        
        // 일정 정보
        if (reservation.getSchedule() != null) {
            dto.scheduleId = reservation.getSchedule().getScheduleId();
            dto.scheduleDate = reservation.getSchedule().getDate();
            dto.startTime = reservation.getSchedule().getStartTime();
            dto.endTime = reservation.getSchedule().getEndTime();
        }
        
        // 티켓 정보
        dto.ticketId = reservation.getTicket().getTicketId();
        dto.ticketName = reservation.getTicket().getName();
        dto.ticketDescription = reservation.getTicket().getDescription();
        dto.ticketPrice = reservation.getTicket().getPrice();
        
        // 예약자 정보
        dto.userId = reservation.getUser().getUserId();
        dto.userName = reservation.getUser().getName();
        dto.userEmail = reservation.getUser().getEmail();
        
        // 예약 정보
        dto.quantity = reservation.getQuantity();
        dto.price = reservation.getPrice();
        dto.reservationStatus = reservation.getReservationStatusCode().getName();
        dto.createdAt = reservation.getCreatedAt();
        dto.updatedAt = reservation.getUpdatedAt();
        dto.canceled = reservation.isCanceled();
        dto.canceledAt = reservation.getCanceled_at();

        return dto;
    }
    
    // 기존 생성자와의 호환성을 위한 생성자 (deprecated)
    @Deprecated
    public ReservationResponseDto(Object event, Object schedule, Object ticket, Object user, int quantity, int price) {
        this.quantity = quantity;
        this.price = price;
        // 기존 코드와의 호환성을 위해 유지하되, 실제로는 from() 메서드 사용 권장
    }
}
