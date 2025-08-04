package com.fairing.fairplay.event.controller;

import com.fairing.fairplay.event.dto.*;
import com.fairing.fairplay.event.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;

    // 행사 등록
    @PostMapping
    public ResponseEntity<EventResponseDto> createEvent(@RequestBody EventRequestDto eventRequestDto) {
        // TODO: 전체 관리자 권한 확인 추가
        EventResponseDto responseDto = eventService.createEvent(eventRequestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    // 행사 상세 생성
    @PostMapping("/{eventId}/details")
    public ResponseEntity<EventDetailResponseDto> createEventDetail(@PathVariable Long eventId, @RequestBody EventDetailRequestDto eventDetailRequestDto) {
        EventDetailResponseDto responseDto = eventService.createEventDetail(eventDetailRequestDto, eventId);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    // 행사 상세 업데이트
    @PatchMapping("/{eventId}/details")
    public ResponseEntity<EventDetailResponseDto> updateEventDetail(@PathVariable Long eventId, @RequestBody EventDetailRequestDto eventDetailRequestDto) {
        EventDetailResponseDto responseDto = eventService.updateEventDetail(eventDetailRequestDto, eventId);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    // 행사 목록 조회 (메인페이지, 검색 등) - EventDetail 정보 등록해야 보임
    @GetMapping
    public ResponseEntity<EventSummaryResponseDto> getEvents(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        EventSummaryResponseDto response = eventService.getEvents(pageable);
        return ResponseEntity.ok(response);
    }

    // 행사 목록 조회 (테스트용)
    @GetMapping("/list")
    public ResponseEntity<List<EventResponseDto>> getEventList() {
        List<EventResponseDto> events = eventService.getEventList();
        return ResponseEntity.ok(events);
    }

    // 행사 상세 조회
    @GetMapping("/{eventId}/details")
    public ResponseEntity<EventDetailResponseDto> getEventDetail(@PathVariable Long eventId) {
        EventDetailResponseDto eventDetail = eventService.getEventDetail(eventId);
        return ResponseEntity.ok(eventDetail);
    }

    // 행사명 및 숨김 상태 업데이트
    @PatchMapping("/{eventId}")
    public ResponseEntity<EventResponseDto> updateEvent(@PathVariable("eventId") Long eventId, @RequestBody EventRequestDto eventRequestDto /* , Auth */) {
        // TODO: 전체 관리자 or 행사 관리자 권한 확인 추가
        EventResponseDto responseDto = eventService.updateEvent(eventId, eventRequestDto, 2L);  // TODO: managerId 수정자 ID로 변경
        return ResponseEntity.ok(responseDto);
    }


}
