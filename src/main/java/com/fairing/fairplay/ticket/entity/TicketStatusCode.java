package com.fairing.fairplay.ticket.entity;

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
@Table(name = "ticket_status_code")
public class TicketStatusCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_status_code_id")
    private Integer ticketStatusCodeId;

    @Column(length = 20, nullable = false, unique = true)
    private String code;

    @Column(length = 50, nullable = false)
    private String name;
}
