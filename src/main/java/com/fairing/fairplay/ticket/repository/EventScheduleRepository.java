package com.fairing.fairplay.ticket.repository;

import com.fairing.fairplay.ticket.entity.EventSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventScheduleRepository extends JpaRepository<EventSchedule, Long> {

    List<EventSchedule> findAllByEvent_EventId(Long eventId);

    Optional<EventSchedule> findByEvent_EventIdAndScheduleId(Long eventId, Long scheduleId);

}
