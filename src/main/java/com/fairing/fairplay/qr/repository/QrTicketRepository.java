package com.fairing.fairplay.qr.repository;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.qr.dto.QrTicketResponseDto;
import com.fairing.fairplay.qr.entity.QrTicket;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QrTicketRepository extends JpaRepository<QrTicket, Long> {

  @Query("""
          SELECT new com.fairing.fairplay.qr.dto.QrTicketResponseDto(
            q.id,
            e.titleKr,
            l.buildingName,
            l.address,
            q.ticketNo,
            q.qrCode,
            q.manualCode)
        FROM QrTicket q
        JOIN q.eventSchedule es
        JOIN es.event e
        JOIN e.eventDetail ed
        JOIN ed.location l
        WHERE q.id = :qrTicketId
      """)
  Optional<QrTicketResponseDto> findDtoById(@Param("qrTicketId") Long qrTicketId);

  Optional<QrTicket> findByAttendee(Attendee attendee);

  Optional<QrTicket> findByTicketNo(String ticketNo);

  @Query("SELECT t FROM QrTicket t WHERE t.attendee.id IN :attendeeIds AND t.attendee.reservation.reservationId IN :reservationIds")
  List<QrTicket> findByAttendeeIdsAndReservationIds(
      @Param("attendeeIds") Set<Long> attendeeIds,
      @Param("reservationIds") Set<Long> reservationIds
  );

  Optional<QrTicket> findByQrCode(String qrCode);

  Optional<QrTicket> findByManualCode(String manualCode);

  boolean existsByQrCode(String qrCode);

  boolean existsByManualCode(String manualCode);
}
