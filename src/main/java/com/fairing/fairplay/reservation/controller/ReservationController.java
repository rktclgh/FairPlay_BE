package com.fairing.fairplay.reservation.controller;

import com.fairing.fairplay.attendee.dto.AttendeeInfoResponseDto;
import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.reservation.dto.ReservationAttendeeDto;
import com.fairing.fairplay.reservation.dto.ReservationRequestDto;
import com.fairing.fairplay.reservation.dto.ReservationResponseDto;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // 박람회(행사) 예약 신청
    @PostMapping
    public ResponseEntity<ReservationResponseDto> createReservation(@RequestBody ReservationRequestDto requestDto,
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long paymentId
                                                                    ) {
        Long userId = userDetails.getUserId();
        requestDto.setEventId(eventId);
        Reservation reservation = reservationService.createReservation(requestDto, userId, paymentId);

        if (reservation == null) {
            throw new IllegalStateException("예약 생성에 실패했습니다.");
        }

        ReservationResponseDto response = ReservationResponseDto.from(reservation);

        return ResponseEntity.ok(response);
    }

    // 박람회(행사) 예약 상세 조회
    @GetMapping("/{reservationId}")
    public ResponseEntity<ReservationResponseDto> getReservationById(@PathVariable Long eventId,
            @PathVariable Long reservationId) {

        Reservation reservation = reservationService.getReservationById(reservationId);

        if (reservation == null) {
            throw new IllegalStateException("예약 조회에 실패했습니다.");
        }

        ReservationResponseDto response = ReservationResponseDto.from(reservation);

        return ResponseEntity.ok(response);
    }

    // 박람회(행사)의 전체 예약 조회 (행사 관리자)
    @GetMapping
    @FunctionAuth("getReservations")
    public ResponseEntity<List<ReservationResponseDto>> getReservations(@PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Reservation> reservations = reservationService.getReservationsByEvent(eventId);

        if (reservations == null) {
            throw new IllegalStateException("예약 조회에 실패했습니다.");
        }

        List<ReservationResponseDto> response = reservations.stream()
                .map(ReservationResponseDto::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    // 예약 수정
    @PutMapping("/{reservationId}")
    public ResponseEntity<ReservationResponseDto> updateReservation(@RequestBody ReservationRequestDto requestDto,
            @PathVariable Long eventId,
            @PathVariable Long reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        requestDto.setEventId(eventId);
        requestDto.setReservationId(reservationId);

        Reservation reservation = reservationService.updateReservation(requestDto, userId);

        ReservationResponseDto response = ReservationResponseDto.from(reservation);

        return ResponseEntity.ok(response);
    }

    // 예약자 명단 조회 (행사 관리자용) - 페이지네이션 지원
    @GetMapping("/attendees")
    @FunctionAuth("getReservationAttendees")
    public ResponseEntity<Page<ReservationAttendeeDto>> getReservationAttendees(
            @PathVariable Long eventId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Long reservationId,
            @PageableDefault(size = 15, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Page<ReservationAttendeeDto> attendees = reservationService.getReservationAttendees(
                eventId, status, name, phone, reservationId, pageable);
        return ResponseEntity.ok(attendees);
    }

    // 예약자 명단 엑셀 다운로드 (행사 관리자용)
    @GetMapping("/attendees/excel")
    @FunctionAuth("downloadAttendeesExcel")
    public ResponseEntity<byte[]> downloadAttendeesExcel(@PathVariable Long eventId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {
        byte[] excelData = reservationService.generateAttendeesExcel(eventId, status);

        String filename = "event_" + eventId + "_attendees.xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    // 예약 취소
    @PatchMapping("/{reservationId}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long eventId,
            @PathVariable Long reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        reservationService.cancelReservation(reservationId, userId);
        return ResponseEntity.noContent().build();
    }
}