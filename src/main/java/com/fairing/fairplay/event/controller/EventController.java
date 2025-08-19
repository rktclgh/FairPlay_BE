package com.fairing.fairplay.event.controller;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.dto.*;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.event.service.EventService;
import com.fairing.fairplay.history.etc.ChangeEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;
    private final EventRepository eventRepository;
    private final S3Client s3client;

    @Value("${cloud.aws.s3.bucket-name}")
    private String bucket;

    private static final Integer ADMIN = 1; // 전체 관리자
    private static final Integer EVENT = 2; // 행사 관리자
    private static final Integer BOOTH = 3; // 부스 관리자
    private static final Integer COMMON = 4; // 일반 사용자

    // 권한 코드 상수 (DB의 user_role_code.code와 일치)
    private static final String ADMIN_ROLE = "ADMIN"; // 전체 관리자
    private static final String EVENT_MANAGER_ROLE = "EVENT_MANAGER"; // 행사 담당자
    private static final String BOOTH_MANAGER_ROLE = "BOOTH_MANAGER"; // 부스 담당자
    private static final String COMMON_ROLE = "COMMON"; // 일반 사용자

    /*********************** CREATE ***********************/
    // 행사 등록 -> EventApply 에서 처리
//    @PostMapping
//    @FunctionAuth("createEvent")
//    public ResponseEntity<EventResponseDto> createEvent(
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            @RequestBody EventRequestDto eventRequestDto) {
//        // 전체 관리자 권한
//        checkAuth(userDetails, ADMIN);
//
//        EventResponseDto responseDto = eventService.createEvent(userDetails.getUserId(), eventRequestDto);
//        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
//    }

    // 행사 상세 생성 -> EventApply 에서 처리
//    @PostMapping("/{eventId}/details")
//    @FunctionAuth("createEventDetail")
//    public ResponseEntity<EventDetailResponseDto> createEventDetail(
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            @PathVariable Long eventId, @RequestBody EventDetailRequestDto eventDetailRequestDto) {
//
//        // 전체 관리자 OR 행사 관리자 권한
//        checkAuth(userDetails, EVENT);
//        Long loginUserId = userDetails.getUserId();
//
//        checkEventManager(userDetails, eventId);
//
//        EventDetailResponseDto responseDto = eventService.createEventDetail(loginUserId, eventDetailRequestDto,
//                eventId);
//        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
//    }

    /*********************** READ ***********************/

    // 사용자 권한 조회 API (프론트엔드에서 복잡한 권한 체크 대신 사용)
    @GetMapping("/user/role")
    public ResponseEntity<Map<String, Object>> getCurrentUserRole(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Map<String, Object> roleInfo = Map.of(
                "roleCode", userDetails.getRoleCode(),
                "userId", userDetails.getUserId(),
                "isAdmin", ADMIN_ROLE.equals(userDetails.getRoleCode()),
                "isEventManager", EVENT_MANAGER_ROLE.equals(userDetails.getRoleCode()),
                "isBoothManager", BOOTH_MANAGER_ROLE.equals(userDetails.getRoleCode()));
        return ResponseEntity.ok(roleInfo);
    }

    // 특정 행사에 대한 권한 확인 API
    @GetMapping("/{eventId}/permission")
    @FunctionAuth("checkEventPermission")
    public ResponseEntity<Map<String, Boolean>> checkEventPermission(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId) {
        try {
            checkEventManagerRole(userDetails, eventId);
            return ResponseEntity.ok(Map.of(
                    "canManage", true,
                    "isAdmin", ADMIN_ROLE.equals(userDetails.getRoleCode()),
                    "isEventManager", EVENT_MANAGER_ROLE.equals(userDetails.getRoleCode())));
        } catch (CustomException e) {
            return ResponseEntity.ok(Map.of(
                    "canManage", false,
                    "isAdmin", ADMIN_ROLE.equals(userDetails.getRoleCode()),
                    "isEventManager", EVENT_MANAGER_ROLE.equals(userDetails.getRoleCode())));
        }
    }

    // 행사 목록 조회 (메인페이지, 검색 등) - EventDetail 정보 등록해야 보임
    @GetMapping
    public ResponseEntity<EventSummaryResponseDto> getEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer mainCategoryId,
            @RequestParam(required = false) Integer subCategoryId,
            @RequestParam(required = false) String regionName,
            @RequestParam(required = false) Boolean includeHidden,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (includeHidden == true && !ADMIN_ROLE.equals(userDetails.getRoleCode())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        EventSummaryResponseDto response = eventService.getEvents(keyword, mainCategoryId, subCategoryId, regionName,
                fromDate, toDate, includeHidden, pageable);
        return ResponseEntity.ok(response);
    }

    // 사용자 담당 이벤트 조회
    @GetMapping("/my-event")
    public ResponseEntity<EventResponseDto> getMyEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        checkAuth(userDetails, EVENT);

        EventResponseDto event = eventService.getUserEvent(userDetails);
        return ResponseEntity.ok(event);
    }

    // 행사 상세 조회
    @GetMapping("/{eventId}/details")
    public ResponseEntity<EventDetailResponseDto> getEventDetail(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        EventDetailResponseDto eventDetail = eventService.getEventDetail(eventId, userDetails);
        return ResponseEntity.ok(eventDetail);
    }

    /*********************** UPDATE ***********************/
    // 썸네일 및 숨김 상태 업데이트
    @PatchMapping("/{eventId}")
    @FunctionAuth("updateEvent")
    public ResponseEntity<EventResponseDto> updateEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("eventId") Long eventId, @RequestBody EventStatusThumbnailDto eventRequestDto /* , Auth */) {

        // 전체 관리자 OR 행사 관리자 권한
        checkAuth(userDetails, EVENT);
        Long loginUserId = userDetails.getUserId();

        checkEventManager(userDetails, eventId);

        EventResponseDto responseDto = eventService.updateEvent(eventId, eventRequestDto, loginUserId);
        return ResponseEntity.ok(responseDto);
    }

    // 행사 상세 업데이트
    @PatchMapping("/{eventId}/details")
    @FunctionAuth("updateEventDetail")
    @ChangeEvent("행사 수정")
    public ResponseEntity<EventDetailResponseDto> updateEventDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId, @RequestBody EventDetailRequestDto eventDetailRequestDto) {

        // 전체 관리자 OR 행사 관리자 권한
        checkAuth(userDetails, EVENT);
        Long loginUserId = userDetails.getUserId();

        checkEventManager(userDetails, eventId);

        EventDetailResponseDto responseDto = eventService.updateEventDetail(loginUserId, eventDetailRequestDto,
                eventId);
        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    /*********************** DELETE ***********************/
    // 행사 소프트 딜리트 (hidden + isDeleted 처리)
    @DeleteMapping("/{eventId}")
    @FunctionAuth("softDeleteEvent")
    @ChangeEvent("행사 소프트 삭제")
    public ResponseEntity<String> softDeleteEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("eventId") Long eventId) {
        checkAuth(userDetails, ADMIN);

        eventService.softDeleteEvent(eventId, userDetails.getUserId());

        return ResponseEntity.ok("행사 삭제 완료 : " + eventId);
    }

    // 행사 삭제 (전체 관리자가 행사 잘못 생성했을 경우 등)
    @DeleteMapping("/{eventId}/hard")
    @FunctionAuth("deleteEvent")
    @ChangeEvent("행사 삭제")
    public ResponseEntity<String> deleteEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("eventId") Long eventId) {
        checkAuth(userDetails, ADMIN);

        eventService.deleteEvent(eventId);

        return ResponseEntity.ok("행사 삭제 완료 : " + eventId);
    }

    // 행사 강제 삭제
    @DeleteMapping("/{eventId}/force")
    @FunctionAuth("forcedDeleteEvent")
    @ChangeEvent("행사 강제 삭제")
    public ResponseEntity<String> forcedDeleteEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("eventId") Long eventId) {
        checkAuth(userDetails, ADMIN);

        eventService.forcedDeleteEvent(eventId);

        return ResponseEntity.ok("행사 삭제 완료 : " + eventId);
    }

    /*********************** 헬퍼 메소드 ***********************/
    private void checkAuth(CustomUserDetails userDetails, Integer authority) {
        log.info("기본 권한 확인");
        log.info("userDetails RoleId: {}", userDetails.getRoleId());
        log.info("authority RoleId: {}", authority);
        if (userDetails.getRoleId() > authority) {
            throw new CustomException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
    }

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

    private void checkEventManager(CustomUserDetails userDetails, Long eventId) {
        log.info("행사 관리자 추가 권한 확인");
        Long managerId = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 행사를 찾을 수 없습니다."))
                .getManager().getUserId();

        Integer authority = userDetails.getRoleId();

        if (!authority.equals(ADMIN) && !managerId.equals(userDetails.getUserId())) {
            log.warn("권한 없는 이벤트 접근 시도 - 사용자 ID: {}, 이벤트 ID: {}, 매니저 ID: {}",
                    userDetails.getUserId(), eventId, managerId);
            throw new CustomException(HttpStatus.FORBIDDEN, "권한이 없습니다.");
        }
    }

}
