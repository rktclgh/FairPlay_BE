package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.dto.CalendarEventDto;
import com.fairing.fairplay.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<Event, Long> {

    @Query("""
        select new com.fairing.fairplay.event.dto.CalendarEventDto(
            e.eventId,
            e.titleKr,
            ed.startDate,
            ed.endDate,
            r.name
        )
        from Event e
        join e.eventDetail ed
        join ed.regionCode r
        where ed.startDate <= :end
          and ed.endDate   >= :start
          and e.isDeleted = false
          order by ed.startDate asc, e.eventId asc
    """)
    List<CalendarEventDto> findEventsByMonth(
            @Param("start") LocalDate start,
            @Param("end")   LocalDate end
    );
}
