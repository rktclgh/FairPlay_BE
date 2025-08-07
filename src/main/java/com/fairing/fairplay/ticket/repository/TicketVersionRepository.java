package com.fairing.fairplay.ticket.repository;

import com.fairing.fairplay.ticket.entity.TicketVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketVersionRepository extends JpaRepository<TicketVersion, Long> {

    @Query("SELECT MAX(tv.versionNumber) FROM TicketVersion tv WHERE tv.ticket.ticketId = :ticketId")
    Integer findMaxVersionByTicket(@Param("ticketId") Long ticketId);

}
