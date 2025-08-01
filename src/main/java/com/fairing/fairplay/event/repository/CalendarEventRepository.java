package com.fairing.fairplay.event.repository;

import com.fairing.fairplay.event.dto.CalendarEventDto;
import com.fairing.fairplay.event.entity.EventDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<EventDetail, Long> {

    @Query("SELECT new com.fairing.fairplay.event.dto.CalendarEventDto(" +
            "e.eventId, e.titleKr, ed.startDate, ed.endDate, r.name) " +
            "FROM Event e " +
            "JOIN EventDetail ed ON e.eventId = ed.eventDetailId " +
            "JOIN RegionCode r ON ed.regionCode.regionCodeId = r.regionCodeId " +
            "WHERE ed.startDate BETWEEN :start AND :end")
    List<CalendarEventDto> findEventsByMonth(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
