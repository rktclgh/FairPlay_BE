package com.fairing.fairplay.shareticket.repository;

import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.shareticket.entity.ShareTicket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShareTicketRepository extends JpaRepository<ShareTicket, Long> {

  Optional<ShareTicket> findByLinkToken(String linkToken);

  List<ShareTicket> findAllByExpiredAtLessThan(LocalDateTime endDate,
      Pageable pageable);

  @Query("SELECT s FROM ShareTicket s " +
      "WHERE s.expired = false " + // 아직 만료되지 않은 티켓
      "AND s.expiredAt < :endDate " + // 오늘 만료 예정
      "AND s.reservation.schedule.date <> :today "+
      "ORDER BY s.expiredAt ASC") // 오늘 예약은 제외
  List<ShareTicket> findAllExpiredExceptTodayReservations(
      @Param("endDate") LocalDateTime endDate,
      @Param("today") LocalDate today,
      Pageable pageable
  );

  boolean existsByLinkToken(String token);

  boolean existsByReservation_ReservationId(Long reservationId);

  Optional<ShareTicket> findByReservation_ReservationId(Long reservationId);

  Optional<ShareTicket> findByReservation(Reservation reservation);
}
