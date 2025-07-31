package com.fairing.fairplay.qr.repository;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.qr.entity.QrTicket;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrTicketRepository extends JpaRepository<QrTicket, Long> {

  Optional<QrTicket> findByAttendee(Attendee attendee);
}
