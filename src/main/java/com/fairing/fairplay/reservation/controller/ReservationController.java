package com.fairing.fairplay.reservation.controller;


import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.reservation.dto.ReservationRequestDto;
import com.fairing.fairplay.reservation.dto.ReservationResponseDto;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService  reservationService;

    // 박람회(행사) 예약 신청
    @PostMapping
    public ResponseEntity<ReservationResponseDto> createReservation(@RequestBody ReservationRequestDto requestDto,
                                                                    @PathVariable Long eventId,
                                                                    @AuthenticationPrincipal CustomUserDetails userDetails)
    {
        Long userId = userDetails.getUserId();
        requestDto.setEventId(eventId);
        Reservation reservation = reservationService.createReservation(requestDto, userId);

        ReservationResponseDto response =  new ReservationResponseDto(
                reservation.getEvent(),
                reservation.getSchedule(),
                reservation.getTicket(),
                reservation.getUser(),
                reservation.getQuantity(),
                reservation.getPrice()
        );

        return ResponseEntity.ok(response);
    }

    // 박람회(행사) 예약 상세 조회
    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponseDto> getReservationById(@PathVariable Long eventId,
                                                                    @RequestBody ReservationRequestDto requestDto,
                                                                     @PathVariable Long reservationId)
    {
        requestDto.setEventId(eventId);
        requestDto.setReservationId(reservationId);

        Reservation reservation = reservationService.getReservationById(requestDto);

        ReservationResponseDto response =  new ReservationResponseDto(
                reservation.getEvent(),
                reservation.getSchedule(),
                reservation.getTicket(),
                reservation.getUser(),
                reservation.getQuantity(),
                reservation.getPrice()
        );

        return ResponseEntity.ok(response);
    }

    // 박람회(행사)의 전체 예약 조회
    @GetMapping
    public ResponseEntity<List<ReservationResponseDto>> getReservations(@PathVariable Long eventId,
                                                                        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<Reservation> reservations = reservationService.getReservationsByEvent(eventId);

        List<ReservationResponseDto> response = reservations.stream()
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
