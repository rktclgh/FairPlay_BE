package com.fairing.fairplay.attendee.controller;

import com.fairing.fairplay.attendee.dto.AttendeeInfoResponseDto;
import com.fairing.fairplay.attendee.dto.AttendeeSaveRequestDto;
import com.fairing.fairplay.attendee.dto.AttendeeUpdateRequestDto;
import com.fairing.fairplay.attendee.service.AttendeeService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/attendees")
public class AttendeeController {

  private final AttendeeService attendeeService;

  // 폼링크 요청
  @PostMapping
  public ResponseEntity<AttendeeInfoResponseDto> saveAttendee(@RequestParam String token,
      @RequestBody AttendeeSaveRequestDto dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(attendeeService.saveGuest(token, dto));
  }

  // 참석자 전체 조회 -> 단체 예약일 경우에만 접근 가능. authenticationprincipal 추가 필요
  @GetMapping("/{reservationId}")
  public ResponseEntity<List<AttendeeInfoResponseDto>> findAll(@PathVariable Long reservationId) {
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
