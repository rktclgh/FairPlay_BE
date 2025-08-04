package com.fairing.fairplay.attendee.repository;

import com.fairing.fairplay.attendee.entity.Attendee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendeeRepository extends JpaRepository<Attendee, Long> {

  List<Attendee> findAllByReservation_ReservationIdOrderByIdAsc(Long reservationId);

  Optional<Attendee> findByReservation_ReservationIdAndAttendeeTypeCode_Id(Long reservationId,
      Integer attendeeTypeCodeId);

  Optional<Attendee> findByIdAndReservation_ReservationIdAndAttendeeTypeCode_Id(Long attendeeId,
      Long reservationId, Integer attendeeTypeCodeId);
}
