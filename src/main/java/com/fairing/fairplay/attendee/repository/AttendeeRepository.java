package com.fairing.fairplay.attendee.repository;

import com.fairing.fairplay.attendee.entity.Attendee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AttendeeRepository extends JpaRepository<Attendee, Long> {

  List<Attendee> findAllByReservation_ReservationIdOrderByIdAsc(Long reservationId);

  Optional<Attendee> findByReservation_ReservationIdAndAttendeeTypeCode_Id(Long reservationId,
      Integer attendeeTypeCodeId);

  Optional<Attendee> findByIdAndReservation_ReservationIdAndAttendeeTypeCode_Id(Long attendeeId,
      Long reservationId, Integer attendeeTypeCodeId);

  Optional<Attendee> findByIdAndReservation_ReservationId(Long attendeeId, Long reservationId);

  Optional<Attendee> findByReservation_ReservationIdAndAttendeeTypeCode_Code(Long reservationId, String attendeeTypeCode);
  // 행사별 예약자 명단 조회 (행사 관리자)
  @Query("""
        SELECT a FROM Attendee a
        JOIN a.reservation r
        JOIN r.event e
        WHERE e.eventId = :eventId
        ORDER BY a.createdAt ASC
    """)
  List<Attendee> findByEventId(Long eventId);
}
