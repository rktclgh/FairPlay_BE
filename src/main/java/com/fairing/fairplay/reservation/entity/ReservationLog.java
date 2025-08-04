package com.fairing.fairplay.reservation.entity;

import com.fairing.fairplay.user.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation_log")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reservationLogId;

    @ManyToOne
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @ManyToOne
    @JoinColumn(name = "reservation_status_code_id")
    private ReservationStatusCode reservationStatusCode;

    @Column(name = "changed_at", nullable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime changedAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private Users changedBy;

    public ReservationLog(Reservation reservation, ReservationStatusCode reservationStatusCode, Users changedBy) {
        this.reservation = reservation;
        this.reservationStatusCode = reservationStatusCode;
        this.changedBy = changedBy;
    }
}
