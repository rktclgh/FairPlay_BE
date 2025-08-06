package com.fairing.fairplay.ticket.controller;

import com.fairing.fairplay.ticket.dto.EventScheduleRequestDto;
import com.fairing.fairplay.ticket.dto.EventScheduleResponseDto;
import com.fairing.fairplay.ticket.service.EventScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events/{eventId}/schedule")
public class EventScheduleController {

    private final EventScheduleService eventScheduleService;

    // 회차 등록
    @PostMapping
    public ResponseEntity<EventScheduleResponseDto> createSchedule(
            @PathVariable Long eventId,
            @RequestBody EventScheduleRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventScheduleService.createSchedule(eventId, requestDto));
    }

    // 회차 목록 조회
    @GetMapping
    public ResponseEntity<List<EventScheduleResponseDto>> getSchedules(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventScheduleService.getSchedules(eventId));
    }

    // 회차 상세 조회
    @GetMapping("/{scheduleId}")
    public ResponseEntity<EventScheduleResponseDto> getSchedule(
            @PathVariable Long eventId,
            @PathVariable Long scheduleId) {
        return ResponseEntity.ok(eventScheduleService.getSchedule(eventId, scheduleId));
    }

    // 회차 수정
    @PatchMapping("/{scheduleId}")
    public ResponseEntity<EventScheduleResponseDto> updateSchedule(
            @PathVariable Long eventId,
            @PathVariable Long scheduleId,
            @RequestBody EventScheduleRequestDto requestDto) {
        return ResponseEntity.ok(eventScheduleService.updateSchedule(eventId, scheduleId, requestDto));
    }

    // 회차 삭제
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable Long eventId,
            @PathVariable Long scheduleId) {
        eventScheduleService.deleteSchedule(eventId, scheduleId);
        return ResponseEntity.noContent().build();
    }
}
