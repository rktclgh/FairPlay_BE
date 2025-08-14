package com.fairing.fairplay.ticket.entity;

import com.fairing.fairplay.ticket.dto.EventScheduleRequestDto;
import jakarta.persistence.*;
import lombok.*;

import com.fairing.fairplay.event.entity.Event;
import com.querydsl.core.annotations.QueryTransient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "event_schedule")
public class EventSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "weekday")
    private Integer weekday;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @QueryTransient
    @OneToMany(mappedBy = "eventSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ScheduleTicket> scheduleTickets = new HashSet<>();

    public static EventSchedule from(EventScheduleRequestDto dto){
        EventSchedule eventSchedule = new EventSchedule();
        eventSchedule.setDate(dto.getDate());
        eventSchedule.setStartTime(dto.getStartTime());
        eventSchedule.setEndTime(dto.getEndTime());
        eventSchedule.setWeekday(dto.getWeekday());
        eventSchedule.setCreatedAt(LocalDateTime.now());
        return eventSchedule;
    }
}
