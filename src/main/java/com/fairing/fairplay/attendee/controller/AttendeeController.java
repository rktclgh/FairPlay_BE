package com.fairing.fairplay.attendee.controller;

import com.fairing.fairplay.attendee.dto.AttendeeInfoResponseDto;
import com.fairing.fairplay.attendee.dto.AttendeeListInfoResponseDto;
import com.fairing.fairplay.attendee.dto.AttendeeSaveRequestDto;
import com.fairing.fairplay.attendee.dto.AttendeeUpdateRequestDto;
import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.attendee.service.AttendeeService;

import com.fairing.fairplay.core.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attendees")
public class AttendeeController {

  private final AttendeeService attendeeService;

  // 폼링크 통한 참석자 저장 -> 동반자만. 대표자는 예약과 동시 저장
  @PostMapping
  public ResponseEntity<AttendeeInfoResponseDto> saveAttendee(@RequestParam String token,
      @RequestBody AttendeeSaveRequestDto dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(attendeeService.saveGuest(token, dto));
  }

  // 참석자 전체 조회 -> 단체 예약일 경우에만 접근 가능.
  @GetMapping("/{reservationId}")
  public ResponseEntity<AttendeeListInfoResponseDto> findAll(@PathVariable Long reservationId,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.status(HttpStatus.OK).body(attendeeService.findAll(reservationId, userDetails));
  }

  // 참석자 정보 변경 -> 단체 예약일 경우에만 접근 가능
  @PatchMapping("/{attendeeId}")
  public ResponseEntity<AttendeeInfoResponseDto> updateAttendee(@PathVariable Long attendeeId,
      @RequestBody AttendeeUpdateRequestDto dto,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(attendeeService.updateAttendee(attendeeId, dto, userDetails));
  }

  // 행사별 예약자 명단 조회 (행사 관리자)
  @GetMapping("/events/{eventId}")
  public ResponseEntity<List<AttendeeInfoResponseDto>> getAttendees(@PathVariable Long eventId,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    List<Attendee> attendees = attendeeService.getAttendeesByEvent(eventId,
        userDetails.getUserId());

    List<AttendeeInfoResponseDto> response = attendees.stream()
        .map(attendee -> AttendeeInfoResponseDto.builder()
            .attendeeId(attendee.getId())
            .reservationId(attendee.getReservation().getReservationId())
            .name(attendee.getName())
            .email(attendee.getEmail())
            .phone(attendee.getPhone())
            .build())
        .toList();

    return ResponseEntity.ok(response);
  }
}
