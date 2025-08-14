package com.fairing.fairplay.ticket.repository;

import com.fairing.fairplay.ticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("SELECT t FROM EventTicket et " +
            "JOIN et.ticket t " +
            "LEFT JOIN FETCH t.ticketStatusCode " +
            "WHERE et.event.eventId = :eventId")
    List<Ticket> findTicketsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT t FROM EventTicket et " +
            "JOIN et.ticket t " +
            "LEFT JOIN FETCH t.ticketStatusCode " +
            "LEFT JOIN FETCH t.ticketAudienceType " +
            "LEFT JOIN FETCH t.ticketSeatType " +
            "WHERE et.event.eventId = :eventId " +
            "AND (:audienceType IS NULL OR :audienceType = '' OR t.ticketAudienceType.code = :audienceType) " +
            "AND (:seatType IS NULL OR :seatType = '' OR t.ticketSeatType.code = :seatType) " +
            "AND (:searchTicketName IS NULL OR :searchTicketName = '' OR UPPER(t.name) LIKE UPPER(CONCAT('%', :searchTicketName, '%')))")
    List<Ticket> findTicketsByEventIdWithFilters(@Param("eventId") Long eventId,
                                                  @Param("audienceType") String audienceType,
                                                  @Param("seatType") String seatType,
                                                  @Param("searchTicketName") String searchTicketName);

}
