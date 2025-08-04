package com.fairing.fairplay.attendee.controller;

import com.fairing.fairplay.attendee.dto.AttendeeInfoResponseDto;
import com.fairing.fairplay.attendee.dto.AttendeeListInfoResponseDto;
import com.fairing.fairplay.attendee.dto.AttendeeSaveRequestDto;
import com.fairing.fairplay.attendee.dto.AttendeeUpdateRequestDto;
import com.fairing.fairplay.attendee.service.AttendeeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/attendees")
@Slf4j
public class AttendeeController {

  private final AttendeeService attendeeService;

  // 폼링크 통한 참석자 저장 -> 동반자만. 대표자는 예약과 동시 저장
  @PostMapping
  public ResponseEntity<AttendeeInfoResponseDto> saveAttendee(@RequestParam String token,
      @RequestBody AttendeeSaveRequestDto dto) {
    log.info("attendee save request: {}", token);
    return ResponseEntity.status(HttpStatus.CREATED).body(attendeeService.saveGuest(token, dto));
  }

  // 참석자 전체 조회 -> 단체 예약일 경우에만 접근 가능. authenticationprincipal 추가 필요
  @GetMapping("/{reservationId}")
  public ResponseEntity<AttendeeListInfoResponseDto> findAll(@PathVariable Long reservationId) {
    return ResponseEntity.status(HttpStatus.OK).body(attendeeService.findAll(reservationId));
  }

  // 참석자 정보 변경 -> 단체 예약일 경우에만 접근 가능 authenticationprincipal 추가 필요
  @PatchMapping("/{attendeeId}")
  public ResponseEntity<AttendeeInfoResponseDto> updateAttendee(@PathVariable Long attendeeId,
      @RequestBody AttendeeUpdateRequestDto dto) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(attendeeService.updateAttendee(attendeeId, dto));
  }
}
