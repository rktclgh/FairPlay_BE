package com.fairing.fairplay.payment.repository;

import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByReservationEventEventId(Long eventId);

    void deleteAllByReservationIn(List<Reservation> reservations);

    Optional<Payment> findByMerchantUid(String merchantUid);

    List<Payment> findByEvent_EventId(Long eventId);

    List<Payment> findByUser_UserId(Long userId);
}
