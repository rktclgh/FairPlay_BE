package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventVersionRepository extends JpaRepository<EventVersion, Long> {
    Optional<EventVersion> findTopByEventOrderByVersionNumberDesc(Event event);
}