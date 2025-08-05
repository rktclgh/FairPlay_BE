package com.fairing.fairplay.event.controller;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.dto.*;
import com.fairing.fairplay.event.service.EventService;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;
    private final UserRepository userRepository;

    private static final Integer ADMIN = 1;    // 전체 관리자
    private static final Integer EVENT = 2;    // 행사 관리자
    private static final Integer BOOTH = 3;    // 부스 관리자
    private static final Integer COMMON = 4;   // 일반 사용자

    // 행사 등록
    @PostMapping
    public ResponseEntity<EventResponseDto> createEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody EventRequestDto eventRequestDto
    ) {
        // 전체 관리자 권한
        checkAuth(userDetails, ADMIN);

        EventResponseDto responseDto = eventService.createEvent(eventRequestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    // 행사 상세 생성
    @PostMapping("/{eventId}/details")
    public ResponseEntity<EventDetailResponseDto> createEventDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId, @RequestBody EventDetailRequestDto eventDetailRequestDto) {

        // 전체 관리자 OR 행사 관리자 권한
        checkAuth(userDetails, EVENT);

        EventDetailResponseDto responseDto = eventService.createEventDetail(eventDetailRequestDto, eventId);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    // 행사 상세 업데이트
    @PatchMapping("/{eventId}/details")
    public ResponseEntity<EventDetailResponseDto> updateEventDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId, @RequestBody EventDetailRequestDto eventDetailRequestDto) {

        // 전체 관리자 OR 행사 관리자 권한
        checkAuth(userDetails, EVENT);

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
    public ResponseEntity<EventResponseDto> updateEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("eventId") Long eventId, @RequestBody EventRequestDto eventRequestDto /* , Auth */) {

        // 전체 관리자 OR 행사 관리자 권한
        checkAuth(userDetails, EVENT);

        EventResponseDto responseDto = eventService.updateEvent(eventId, eventRequestDto, 2L);  // TODO: managerId 수정자 ID로 변경
        return ResponseEntity.ok(responseDto);
    }

    /*********************** 헬퍼 메소드 ***********************/
    private void checkAuth(@AuthenticationPrincipal CustomUserDetails userDetails, Integer authority) {
        Long userId = userDetails.getUserId();
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."));

        if (user.getRoleCode().getId() > authority) {
            throw new CustomException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
    }


}
