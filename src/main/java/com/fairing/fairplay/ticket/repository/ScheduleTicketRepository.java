package com.fairing.fairplay.ticket.repository;

import com.fairing.fairplay.ticket.dto.ScheduleTicketResponseDto;
import com.fairing.fairplay.ticket.entity.ScheduleTicket;
import com.fairing.fairplay.ticket.entity.ScheduleTicketId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleTicketRepository extends JpaRepository<ScheduleTicket, ScheduleTicketId> {

    void deleteByEventSchedule_ScheduleId(Long scheduleId);

    @Query("""
    SELECT new com.fairing.fairplay.ticket.dto.ScheduleTicketResponseDto(
            t.ticketId,
            t.name,
            t.price,
            st.remainingStock,
            st.salesStartAt,
            st.salesEndAt,
            st.visible
        )
        FROM EventTicket et
        JOIN et.ticket t
        JOIN et.event e
        JOIN EventSchedule es ON es.event.eventId = e.eventId
        LEFT JOIN ScheduleTicket st
            ON st.eventSchedule.scheduleId = es.scheduleId
           AND st.ticket.ticketId = t.ticketId
        WHERE e.eventId = :eventId
          AND es.scheduleId = :scheduleId
    """)
    List<ScheduleTicketResponseDto> findScheduleTickets(
            @Param("eventId") Long eventId,
            @Param("scheduleId") Long scheduleId
    );

}
