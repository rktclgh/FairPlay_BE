package com.fairing.fairplay.attendee.repository;

import com.fairing.fairplay.attendee.entity.Attendee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendeeRepository extends JpaRepository<Attendee, Long> {

  Optional<List<Attendee>> findAllByReservationId(Long reservationId);

  Optional<Attendee> findByReservationIdAndAttendeeTypeCode_Id(Long reservationId,
      Integer attendeeTypeCodeId);

  Optional<Attendee> findByIdAndReservationIdAndAttendeeTypeCode_Id(Long attendeeId,
      Long reservationId, Integer attendeeTypeCodeId);
}
