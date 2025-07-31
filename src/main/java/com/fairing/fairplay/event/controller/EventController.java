package com.fairing.fairplay.event.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.fairing.fairplay.event.service.EventService;
import com.fairing.fairplay.event.dto.EventRequestDto;
import com.fairing.fairplay.event.dto.EventResponseDto;
import com.fairing.fairplay.event.entity.Event;

import java.util.List;


@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;

    // 행사 등록
    @PostMapping
    public EventResponseDto createEvent(@RequestBody EventRequestDto eventRequestDto) {
        // TODO: 전체 관리자 권한 확인 추가
        return eventService.createEvent(eventRequestDto);
    }

    // 행사 목록 조회 (임시로 id List 받음)
    @GetMapping
    public List<Long> getEvents() {
        return eventService.getEvents();
    }

    // 행사명 및 숨김 상태 업데이트
    @PatchMapping("/{eventId}")
    public EventResponseDto updateEvent(@PathVariable("eventId") Long eventId, @RequestBody EventRequestDto eventRequestDto /* , Auth */) {
        // TODO: 전체 관리자 or 행사 관리자 권한 확인 추가
        return eventService.updateEvent(eventId, eventRequestDto, 2L);  // TODO: managerId 수정자 ID로 변경
    }
}
