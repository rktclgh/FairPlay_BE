package com.fairing.fairplay.event.entity;

import com.fairing.fairplay.user.entity.EventAdmin;
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

    @Column(nullable = false)
    private Boolean hidden = true;
}
