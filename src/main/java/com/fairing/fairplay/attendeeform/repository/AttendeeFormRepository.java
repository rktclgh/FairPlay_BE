package com.fairing.fairplay.attendeeform.repository;

import com.fairing.fairplay.attendeeform.entity.AttendeeForm;
import com.fairing.fairplay.reservation.entity.Reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendeeFormRepository extends JpaRepository<AttendeeForm, Long> {

  Optional<AttendeeForm> findByLinkToken(String linkToken);

  List<AttendeeForm> findAllByExpiredAtLessThan(LocalDateTime endDate,
                                                                                         Pageable pageable);

  @Query("SELECT a FROM AttendeeForm a " +
      "WHERE a.expired = false " + // 아직 만료되지 않은 티켓
      "AND a.expiredAt < :endDate " + // 오늘 만료 예정
      "AND a.reservation.schedule.date <> :today "+
      "ORDER BY a.expiredAt ASC") // 오늘 예약은 제외
  List<AttendeeForm> findAllExpiredExceptTodayReservations(
      @Param("endDate") LocalDateTime endDate,
      @Param("today") LocalDate today,
      Pageable pageable
  );

  boolean existsByLinkToken(String token);

  boolean existsByReservation_ReservationIdAndExpiredFalse(Long reservationId);

  Optional<AttendeeForm> findByReservation_ReservationId(Long reservationId);

  Optional<AttendeeForm> findByReservation(Reservation reservation);

//  boolean existsByReservation_ReservationIdAndExpiredFalse(Long reservationReservationId, Boolean expired);
}
