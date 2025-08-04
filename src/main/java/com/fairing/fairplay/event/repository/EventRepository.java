package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, EventQueryRepository {
    Page<Event> findAll(Pageable pageable);

    List<Event> findAllByEventDetail_StartDateAfter(LocalDate today);

    List<Event> findAllByEventDetail_StartDateLessThanEqualAndEventDetail_EndDateGreaterThanEqual(
            LocalDate start, LocalDate end);

    List<Event> findAllByEventDetail_EndDateBefore(LocalDate today);

    Page<Event> findByHiddenFalseAndEventDetailIsNotNull(Pageable pageable);
}
