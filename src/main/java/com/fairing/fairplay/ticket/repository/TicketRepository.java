package com.fairing.fairplay.ticket.repository;

import com.fairing.fairplay.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("SELECT t FROM EventTicket et " +
            "JOIN et.ticket t " +
            "WHERE et.event.eventId = :eventId")
    List<Ticket> findTicketsByEventId(@Param("eventId") Long eventId);

}
