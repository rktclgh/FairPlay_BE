package com.fairing.fairplay.payment.repository;

import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByReservationEventEventId(Long eventId);

    void deleteAllByReservationIn(List<Reservation> reservations);
}