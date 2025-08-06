package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.ticket.entity.EventSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventScheduleRepository extends JpaRepository<EventSchedule, Long> {
    List<EventSchedule> findByEvent_EventId(Long eventId);
}
