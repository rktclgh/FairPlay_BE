package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventVersion;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventVersionRepository extends JpaRepository<EventVersion, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EventVersion> findTopByEventOrderByVersionNumberDesc(Event event);
    
    Page<EventVersion> findByEventOrderByVersionNumberDesc(Event event, Pageable pageable);
    
    Optional<EventVersion> findByEventAndVersionNumber(Event event, Integer versionNumber);

}