package com.fairing.fairplay.reservation.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.reservation.dto.ReservationResponseDto;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me/reservations")
@RequiredArgsConstructor
public class MyReservationController {

    private final ReservationService reservationService;

    @GetMapping
    public ResponseEntity<List<ReservationResponseDto>> getMyReservations(@AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        List<Reservation> myReservations =  reservationService.getMyReservations(userId);

        List<ReservationResponseDto> response = myReservations.stream()
                .map(r -> new ReservationResponseDto(
                        r.getEvent(),
                        r.getSchedule(),
                        r.getTicket(),
                        r.getUser(),
                        r.getQuantity(),
                        r.getPrice()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }
}
