package com.fairing.fairplay.reservation.repository;

import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.user.entity.Users;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

  List<Reservation> findByEvent_EventId(Long eventId);

  List<Reservation> findByUser_userId(Long userUserId);

  Optional<Reservation> findByReservationIdAndUser(Long reservationId, Users user);
}
