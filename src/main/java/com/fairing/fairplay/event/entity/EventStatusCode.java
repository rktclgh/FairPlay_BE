package com.fairing.fairplay.event.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_status_code")
public class EventStatusCode {
    // UPCOMING / ONGOING / ENDED

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_status_code_id")
    private Integer eventStatusCodeId;

    @Column(length = 20, nullable = false, unique = true)
    private String code;

    @Column(length = 50, nullable = false)
    private String name;

}