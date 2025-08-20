package com.fairing.fairplay.attendee.repository;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.reservation.entity.Reservation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendeeRepository extends JpaRepository<Attendee, Long> {

  List<Attendee> findAllByReservation_ReservationIdOrderByIdAsc(Long reservationId);

  Optional<Attendee> findByReservation_ReservationIdAndAttendeeTypeCode_Id(Long reservationId,
      Integer attendeeTypeCodeId);

  Optional<Attendee> findByIdAndReservation_ReservationIdAndAttendeeTypeCode_Id(Long attendeeId,
      Long reservationId, Integer attendeeTypeCodeId);

  Optional<Attendee> findByIdAndReservation_ReservationId(Long attendeeId, Long reservationId);

  Optional<Attendee> findByReservation_ReservationIdAndAttendeeTypeCode_Code(Long reservationId,
      String attendeeTypeCode);

  // 행사별 예약자 명단 조회 (행사 관리자)
  @Query("""
          SELECT a FROM Attendee a
          JOIN a.reservation r
          JOIN r.event e
          WHERE e.eventId = :eventId
          ORDER BY a.createdAt ASC
      """)
  List<Attendee> findByEventId(Long eventId);

  Attendee findByEmailAndReservation(String email, Reservation reservation);

  // 참가자 목록 조회 (페이지네이션 + 필터링)
  @Query("""
        SELECT a FROM Attendee a 
        JOIN a.reservation r 
        JOIN r.event e 
        JOIN r.user u
        LEFT JOIN r.reservationStatusCode rsc
        WHERE e.eventId = :eventId
          AND (:status IS NULL OR rsc.code = :status)
          AND (:name IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:phone IS NULL OR a.phone LIKE CONCAT('%', :phone, '%'))
          AND (:reservationId IS NULL OR r.reservationId = :reservationId)
        ORDER BY a.createdAt DESC
    """)
  Page<Attendee> findAttendeesWithFilters(
      @Param("eventId") Long eventId,
      @Param("status") String status,
      @Param("name") String name,
      @Param("phone") String phone,
      @Param("reservationId") Long reservationId,
      Pageable pageable);

  // 참가자 목록 조회 (엑셀 다운로드용)
  @Query("""
        SELECT a FROM Attendee a 
        JOIN a.reservation r 
        JOIN r.event e 
        LEFT JOIN r.reservationStatusCode rsc
        WHERE e.eventId = :eventId
          AND (:status IS NULL OR rsc.code = :status)
        ORDER BY a.createdAt DESC
    """)
  List<Attendee> findAttendeesByEventId(
      @Param("eventId") Long eventId,
      @Param("status") String status);
}
