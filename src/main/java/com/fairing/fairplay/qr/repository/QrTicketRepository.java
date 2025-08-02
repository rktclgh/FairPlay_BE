package com.fairing.fairplay.qr.repository;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.qr.dto.QrTicketResponseDto;
import com.fairing.fairplay.qr.entity.QrTicket;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QrTicketRepository extends JpaRepository<QrTicket, Long> {

  @Query("""
          SELECT new com.fairing.fairplay.qr.dto.QrTicketResponseDto(
            e.titleKr,
            l.buildingName,
            q.ticketNo,
            q.qrCode,
            q.manualCode)
        FROM QrTicket q
        JOIN q.eventTicket et
        JOIN et.event e
        JOIN e.eventDetail ed
        JOIN ed.location l
        WHERE q.id = :qrTicketId
      """)
  Optional<QrTicketResponseDto> findDtoById(@Param("qrTicketId") Long qrTicketId);

  Optional<QrTicket> findByAttendee(Attendee attendee);
}
