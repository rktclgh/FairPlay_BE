package com.fairing.fairplay.event.controller;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.dto.*;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.event.service.EventService;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    private static final Integer ADMIN = 1;    // 전체 관리자
    private static final Integer EVENT = 2;    // 행사 관리자
    private static final Integer BOOTH = 3;    // 부스 관리자
    private static final Integer COMMON = 4;   // 일반 사용자


    /*********************** CREATE ***********************/
    // 행사 등록
    @PostMapping
    public ResponseEntity<EventResponseDto> createEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody EventRequestDto eventRequestDto
    ) {
        // 전체 관리자 권한
        checkAuth(userDetails, ADMIN);

        EventResponseDto responseDto = eventService.createEvent(userDetails.getUserId(), eventRequestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    // 행사 상세 생성
    @PostMapping("/{eventId}/details")
    public ResponseEntity<EventDetailResponseDto> createEventDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId, @RequestBody EventDetailRequestDto eventDetailRequestDto) {

        // 전체 관리자 OR 행사 관리자 권한
        checkAuth(userDetails, EVENT);
        Long loginUserId = userDetails.getUserId();

        checkEventManager(userDetails, eventId);

        EventDetailResponseDto responseDto = eventService.createEventDetail(loginUserId, eventDetailRequestDto, eventId);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    /*********************** READ ***********************/
    // 행사 목록 조회 (메인페이지, 검색 등) - EventDetail 정보 등록해야 보임
    @GetMapping
    public ResponseEntity<EventSummaryResponseDto> getEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer mainCategoryId,
            @RequestParam(required = false) Integer subCategoryId,
            @RequestParam(required = false) String regionName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        EventSummaryResponseDto response = eventService.getEvents(keyword, mainCategoryId, subCategoryId, regionName, fromDate, toDate, pageable);
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


    /*********************** UPDATE ***********************/
    // 행사명 및 숨김 상태 업데이트
    @PatchMapping("/{eventId}")
    public ResponseEntity<EventResponseDto> updateEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("eventId") Long eventId, @RequestBody EventRequestDto eventRequestDto /* , Auth */) {

        // 전체 관리자 OR 행사 관리자 권한
        checkAuth(userDetails, EVENT);
        Long loginUserId = userDetails.getUserId();

        checkEventManager(userDetails, eventId);

        EventResponseDto responseDto = eventService.updateEvent(eventId, eventRequestDto, loginUserId);
        return ResponseEntity.ok(responseDto);
    }

    // 행사 상세 업데이트
    @PatchMapping("/{eventId}/details")
    public ResponseEntity<EventDetailResponseDto> updateEventDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId, @RequestBody EventDetailRequestDto eventDetailRequestDto) {

        // 전체 관리자 OR 행사 관리자 권한
        checkAuth(userDetails, EVENT);
        Long loginUserId = userDetails.getUserId();

        checkEventManager(userDetails, eventId);

        EventDetailResponseDto responseDto = eventService.updateEventDetail(loginUserId, eventDetailRequestDto, eventId);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }


    /*********************** DELETE ***********************/
    // 행사 삭제
    @DeleteMapping("/{eventId}")
    public ResponseEntity<String> deleteEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("eventId") Long eventId
    ) {
        checkAuth(userDetails, ADMIN);

        eventService.deleteEvent(eventId);

        return ResponseEntity.ok("행사 삭제 완료 : " + eventId);
    }


    /*********************** 헬퍼 메소드 ***********************/
    private void checkAuth(@AuthenticationPrincipal CustomUserDetails userDetails, Integer authority) {
        log.info("기본 권한 확인");

        if (userDetails.getRoleId() > authority) {
            throw new CustomException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
    }

    private void checkEventManager(@AuthenticationPrincipal CustomUserDetails userDetails, Long eventId) {
        log.info("행사 관리자 추가 권한 확인");
        Long managerId = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 행사를 찾을 수 없습니다."))
                .getManager().getUserId();

        Integer authority = userDetails.getRoleId();

        if (!authority.equals(ADMIN) && !managerId.equals(userDetails.getUserId())) throw new CustomException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
    }


}
