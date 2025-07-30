package com.fairing.fairplay.booth.entity;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.user.entity.BoothAdmin;
import com.fairing.fairplay.ticket.entity.BoothTicket;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor
public class Booth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booth_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_type_id", nullable = false)
    private BoothType boothType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booth_admin_id")
    private BoothAdmin boothAdmin;

    @Column(name = "booth_title", nullable = false, length = 100)
    private String boothTitle;

    @Column(name = "booth_description", nullable = false, columnDefinition = "TEXT")
    private String boothDescription;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(length = 100)
    private String location;

    @Column(name = "created_at", updatable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "booth", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BoothTicket> boothTickets = new HashSet<>();

}