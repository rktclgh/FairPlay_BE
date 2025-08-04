package com.fairing.fairplay.reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservation_status_code")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationStatusCode {

    @Id
    @Column(name = "reservation_status_code_id")
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;
}
