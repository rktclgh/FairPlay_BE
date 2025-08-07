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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    // 권한 코드 상수 (DB의 user_role_code.code와 일치)
    private static final String ADMIN_ROLE = "ADMIN";         // 전체 관리자
    private static final String EVENT_MANAGER_ROLE = "EVENT_MANAGER";   // 행사 담당자
    private static final String BOOTH_MANAGER_ROLE = "BOOTH_MANAGER";   // 부스 담당자
    private static final String COMMON_ROLE = "COMMON";       // 일반 사용자


    /*********************** CREATE ***********************/
    // 행사 등록
    @PostMapping
    public ResponseEntity<EventResponseDto> createEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody EventRequestDto eventRequestDto
    ) {
        // 전체 관리자 권한
        checkAdminRole(userDetails);

        EventResponseDto responseDto = eventService.createEvent(userDetails.getUserId(), eventRequestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    // 행사 상세 생성
    @PostMapping("/{eventId}/details")
    public ResponseEntity<EventDetailResponseDto> createEventDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId, @RequestBody EventDetailRequestDto eventDetailRequestDto) {

        // 전체 관리자 OR 행사 담당자 권한
        checkEventManagerRole(userDetails, eventId);
        Long loginUserId = userDetails.getUserId();

        EventDetailResponseDto responseDto = eventService.createEventDetail(loginUserId, eventDetailRequestDto, eventId);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    /*********************** READ ***********************/
    
    // 사용자 권한 조회 API (프론트엔드에서 복잡한 권한 체크 대신 사용)
    @GetMapping("/user/role")
    public ResponseEntity<Map<String, Object>> getCurrentUserRole(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Map<String, Object> roleInfo = Map.of(
            "roleCode", userDetails.getRoleCode(),
            "userId", userDetails.getUserId(),
            "isAdmin", ADMIN_ROLE.equals(userDetails.getRoleCode()),
            "isEventManager", EVENT_MANAGER_ROLE.equals(userDetails.getRoleCode()),
            "isBoothManager", BOOTH_MANAGER_ROLE.equals(userDetails.getRoleCode())
        );
        return ResponseEntity.ok(roleInfo);
    }
    
    // 특정 행사에 대한 권한 확인 API
    @GetMapping("/{eventId}/permission")
    public ResponseEntity<Map<String, Boolean>> checkEventPermission(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId
    ) {
        try {
            checkEventManagerRole(userDetails, eventId);
            return ResponseEntity.ok(Map.of(
                "canManage", true,
                "isAdmin", ADMIN_ROLE.equals(userDetails.getRoleCode()),
                "isEventManager", EVENT_MANAGER_ROLE.equals(userDetails.getRoleCode())
            ));
        } catch (CustomException e) {
            return ResponseEntity.ok(Map.of(
                "canManage", false,
                "isAdmin", ADMIN_ROLE.equals(userDetails.getRoleCode()),
                "isEventManager", EVENT_MANAGER_ROLE.equals(userDetails.getRoleCode())
            ));
        }
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


    /*********************** UPDATE ***********************/
    // 행사명 및 숨김 상태 업데이트
    @PatchMapping("/{eventId}")
    public ResponseEntity<EventResponseDto> updateEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("eventId") Long eventId, @RequestBody EventRequestDto eventRequestDto /* , Auth */) {

        // 전체 관리자 OR 행사 담당자 권한
        checkEventManagerRole(userDetails, eventId);
        Long loginUserId = userDetails.getUserId();

        EventResponseDto responseDto = eventService.updateEvent(eventId, eventRequestDto, loginUserId);
        return ResponseEntity.ok(responseDto);
    }

    // 행사 상세 업데이트
    @PatchMapping("/{eventId}/details")
    public ResponseEntity<EventDetailResponseDto> updateEventDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId, @RequestBody EventDetailRequestDto eventDetailRequestDto) {

        // 전체 관리자 OR 행사 담당자 권한
        checkEventManagerRole(userDetails, eventId);
        Long loginUserId = userDetails.getUserId();

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
        checkAdminRole(userDetails);

        eventService.deleteEvent(eventId);

        return ResponseEntity.ok("행사 삭제 완료 : " + eventId);
    }


    /*********************** 헬퍼 메소드 ***********************/
    
    // 전체 관리자 권한 확인
    private void checkAdminRole(CustomUserDetails userDetails) {
        log.info("전체 관리자 권한 확인 - 사용자 역할: {}", userDetails.getRoleCode());
        
        if (!ADMIN_ROLE.equals(userDetails.getRoleCode())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "전체 관리자만 접근할 수 있습니다.");
        }
    }

    // 행사 담당자 권한 확인 (전체 관리자 OR 해당 행사의 담당자)
    private void checkEventManagerRole(CustomUserDetails userDetails, Long eventId) {
        log.info("행사 담당자 권한 확인 - 사용자 역할: {}, 사용자 ID: {}", 
                 userDetails.getRoleCode(), userDetails.getUserId());

        String userRole = userDetails.getRoleCode();
        
        // 전체 관리자는 모든 행사에 접근 가능
        if (ADMIN_ROLE.equals(userRole)) {
            return;
        }
        
        // 행사 담당자인 경우, 자신이 담당하는 행사만 접근 가능
        if (EVENT_MANAGER_ROLE.equals(userRole)) {
            Long managerId = eventRepository.findById(eventId)
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 행사를 찾을 수 없습니다."))
                    .getManager().getUserId();
            
            if (managerId.equals(userDetails.getUserId())) {
                return;
            }
        }
        
        throw new CustomException(HttpStatus.FORBIDDEN, "해당 행사에 대한 권한이 없습니다.");
    }


}
