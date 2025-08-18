package com.fairing.fairplay.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.event.entity.Event;

import java.util.List;

public interface EventAdminRepository extends JpaRepository<EventAdmin, Long> {
    
    @Query("SELECT e FROM Event e WHERE e.manager = :eventAdmin")
    List<Event> findEventsByManager(@Param("eventAdmin") EventAdmin eventAdmin);
    
    @Query("SELECT e.manager FROM Event e WHERE e = :event AND e.manager IS NOT NULL")
    EventAdmin findByEvent(@Param("event") Event event);
}
