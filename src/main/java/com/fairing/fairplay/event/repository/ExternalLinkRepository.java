package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.ExternalLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExternalLinkRepository extends JpaRepository<ExternalLink, Long> {
    List<ExternalLink> findByEvent(Event event);

    void deleteByEvent(Event event);
}
