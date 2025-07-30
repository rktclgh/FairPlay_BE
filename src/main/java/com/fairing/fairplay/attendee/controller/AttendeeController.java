package com.fairing.fairplay.attendee.controller;

import com.fairing.fairplay.attendee.dto.AttendeeSaveRequestDto;
import com.fairing.fairplay.attendee.dto.AttendeeSaveResponseDto;
import com.fairing.fairplay.attendee.service.AttendeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/attendees")
public class AttendeeController {

  private final AttendeeService attendeeService;

  // 폼링크 요청
  @PostMapping
  public ResponseEntity<?> saveAttendee(@RequestParam String token,
      @RequestBody AttendeeSaveRequestDto dto) {
    attendeeService.saveGuest(token, dto);
    return ResponseEntity.ok(new AttendeeSaveResponseDto());
  }
}
