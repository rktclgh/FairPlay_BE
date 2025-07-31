package com.fairing.fairplay.event.entity;

import com.fairing.fairplay.booth.entity.Booth;
import com.fairing.fairplay.ticket.entity.EventTicket;
import com.fairing.fairplay.ticket.entity.Ticket;
import com.fairing.fairplay.user.entity.EventAdmin;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private EventAdmin manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_code_id", nullable = false)
    private EventStatusCode statusCode;

    @Column(name = "event_code", unique = true, nullable = false, length = 50)
    private String eventCode;

    @Column(name = "title_kr", nullable = false, length = 200)
    private String titleKr;

    @Column(name = "title_eng", nullable = false, length = 200)
    private String titleEng;

    @Column(nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private Boolean hidden = true;

    @OneToOne(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private EventDetail eventDetail;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ExternalLink> externalLinks = new HashSet<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EventVersion> eventVersions = new HashSet<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EventTicket> eventTickets = new HashSet<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Booth> booths = new HashSet<>();
}