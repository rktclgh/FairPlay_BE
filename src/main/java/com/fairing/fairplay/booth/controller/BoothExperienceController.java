package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.*;
import com.fairing.fairplay.booth.entity.Booth;
import com.fairing.fairplay.booth.service.BoothExperienceService;
import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "부스 체험 관리", description = "부스 체험 등록, 예약, 상태 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/booth-experiences")
@RequiredArgsConstructor
public class BoothExperienceController {

    private final BoothExperienceService boothExperienceService;

    // 1. 부스 체험 등록 (부스 담당자)
    @Operation(summary = "부스 체험 등록", description = "부스 담당자가 새로운 체험을 등록합니다.")
    @PostMapping("/booths/{boothId}")
    @FunctionAuth("createBoothExperience")
    public ResponseEntity<BoothExperienceResponseDto> createBoothExperience(
            @Parameter(description = "부스 ID") @PathVariable Long boothId,
            @RequestBody BoothExperienceRequestDto requestDto) {

        log.info("부스 체험 등록 요청 - 부스 ID: {}", boothId);
        BoothExperienceResponseDto response = boothExperienceService.createBoothExperience(boothId, requestDto);
        return ResponseEntity.ok(response);
    }

    // 2. 부스 체험 목록 조회 (권한 기반)
    @Operation(summary = "부스 체험 목록 조회", description = "사용자 권한에 따라 관리 가능한 부스 체험 목록을 조회합니다.")
    @GetMapping("/booths/{boothId}")
    @FunctionAuth("getBoothExperiences")
    public ResponseEntity<List<BoothExperienceResponseDto>> getBoothExperiences(
            @Parameter(description = "부스 ID") @PathVariable Long boothId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("부스 체험 목록 조회 - 부스 ID: {}, 사용자 ID: {}, 권한: {}", 
                boothId, userDetails.getUserId(), userDetails.getRoleCode());
        
        List<BoothExperienceResponseDto> experiences = boothExperienceService.getBoothExperiences(
                boothId, userDetails.getUserId(), userDetails.getRoleCode());
        return ResponseEntity.ok(experiences);
    }

    // 3. 체험 가능한 부스 체험 목록 조회 (참여자용)
    @Operation(summary = "부스 체험 목록 조회", description = "필터 조건에 따라 부스 체험을 조회합니다. isAvailable=true면 예약 가능한 것만, null이면 모든 체험을 조회합니다.")
    @GetMapping("/available")
    public ResponseEntity<List<BoothExperienceResponseDto>> getAvailableExperiences(
            @Parameter(description = "이벤트 ID") @RequestParam(required = false) Long eventId,
            @Parameter(description = "기간 시작일 (YYYY-MM-DD)") @RequestParam(required = false) String startDate,
            @Parameter(description = "기간 종료일 (YYYY-MM-DD)") @RequestParam(required = false) String endDate,
            @Parameter(description = "부스명 또는 체험명 검색") @RequestParam(required = false) String boothName,
            @Parameter(description = "예약 가능 여부 필터 (true=예약가능만, null=모든체험)") @RequestParam(required = false) Boolean isAvailable,
            @Parameter(description = "카테고리 ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "startTime") String sortBy,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "asc") String sortDirection) {

        log.info("부스 체험 목록 조회 - 필터: eventId={}, startDate={}, endDate={}, boothName={}, isAvailable={}, sortBy={}", 
                eventId, startDate, endDate, boothName, isAvailable, sortBy);
        
        List<BoothExperienceResponseDto> experiences = boothExperienceService.getAvailableExperiences(
                eventId, startDate, endDate, boothName, isAvailable, categoryId, sortBy, sortDirection);
        return ResponseEntity.ok(experiences);
    }

    // 4. 부스 체험 상세 조회
    @Operation(summary = "부스 체험 상세 조회", description = "특정 체험의 상세 정보를 조회합니다.")
    @GetMapping("/{experienceId}")
    public ResponseEntity<BoothExperienceResponseDto> getBoothExperience(
            @Parameter(description = "체험 ID") @PathVariable Long experienceId) {

        log.info("부스 체험 상세 조회 - 체험 ID: {}", experienceId);
        BoothExperienceResponseDto experience = boothExperienceService.getBoothExperience(experienceId);
        return ResponseEntity.ok(experience);
    }

    // 4-1. 부스 체험 수정 (부스 담당자)
    @Operation(summary = "부스 체험 수정", description = "부스 담당자가 기존 체험을 수정합니다.")
    @PutMapping("/{experienceId}")
    @FunctionAuth("updateBoothExperience")
    public ResponseEntity<BoothExperienceResponseDto> updateBoothExperience(
            @Parameter(description = "체험 ID") @PathVariable Long experienceId,
            @RequestBody BoothExperienceRequestDto requestDto) {

        log.info("부스 체험 수정 요청 - 체험 ID: {}", experienceId);
        BoothExperienceResponseDto response = boothExperienceService.updateBoothExperience(experienceId, requestDto);
        return ResponseEntity.ok(response);
    }

    // 4-2. 부스 체험 삭제 (부스 담당자)
    @Operation(summary = "부스 체험 삭제", description = "부스 담당자가 체험을 삭제합니다.")
    @DeleteMapping("/{experienceId}")
    @FunctionAuth("deleteBoothExperience")
    public ResponseEntity<Void> deleteBoothExperience(
            @Parameter(description = "체험 ID") @PathVariable Long experienceId) {

        log.info("부스 체험 삭제 요청 - 체험 ID: {}", experienceId);
        boothExperienceService.deleteBoothExperience(experienceId);
        return ResponseEntity.ok().build();
    }

    // 5. 부스 체험 예약 신청 (참여자)
    @Operation(summary = "부스 체험 예약", description = "사용자가 부스 체험을 예약합니다.")
    @PostMapping("/{experienceId}/reservations")
    public ResponseEntity<BoothExperienceReservationResponseDto> createReservation(
            @Parameter(description = "체험 ID") @PathVariable Long experienceId,
            @Parameter(description = "사용자 ID") @RequestParam Long userId,
            @RequestBody BoothExperienceReservationRequestDto requestDto) {

        log.info("부스 체험 예약 요청 - 체험 ID: {}, 사용자 ID: {}", experienceId, userId);
        BoothExperienceReservationResponseDto response = boothExperienceService.createReservation(
                experienceId, userId, requestDto);
        return ResponseEntity.ok(response);
    }

    // 6. 부스 체험 예약자 목록 조회 (부스 담당자용)
    @Operation(summary = "예약자 목록 조회", description = "특정 체험의 모든 예약자 목록을 조회합니다.")
    @GetMapping("/{experienceId}/reservations")
    @FunctionAuth("getExperienceReservations")
    public ResponseEntity<List<BoothExperienceReservationResponseDto>> getExperienceReservations(
            @Parameter(description = "체험 ID") @PathVariable Long experienceId) {

        log.info("예약자 목록 조회 - 체험 ID: {}", experienceId);
        List<BoothExperienceReservationResponseDto> reservations = boothExperienceService
                .getExperienceReservations(experienceId);
        return ResponseEntity.ok(reservations);
    }

    // 7. 내 예약 목록 조회 (참여자용)
    @Operation(summary = "내 예약 목록", description = "사용자의 모든 예약 내역을 조회합니다.")
    @GetMapping("/my-reservations")
    public ResponseEntity<List<BoothExperienceReservationResponseDto>> getMyReservations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();

        log.info("내 예약 목록 조회 - 사용자 ID: {}", userId);

        List<BoothExperienceReservationResponseDto> reservations = boothExperienceService.getMyReservations(userId);
        return ResponseEntity.ok(reservations);
    }

    // 8. 예약 상태 변경 (부스 담당자)
    @Operation(summary = "예약 상태 변경", description = "부스 담당자가 예약의 상태를 변경합니다.")
    @PutMapping("/reservations/{reservationId}/status")
    @FunctionAuth("updateReservationStatus")
    public ResponseEntity<BoothExperienceReservationResponseDto> updateReservationStatus(
            @Parameter(description = "예약 ID") @PathVariable Long reservationId,
            @RequestBody BoothExperienceStatusUpdateDto updateDto) {

        log.info("예약 상태 변경 - 예약 ID: {}, 상태: {}", reservationId, updateDto.getStatusCode());
        BoothExperienceReservationResponseDto response = boothExperienceService.updateReservationStatus(
                reservationId, updateDto);
        return ResponseEntity.ok(response);
    }

    // 9. 예약 취소 (참여자)
    @Operation(summary = "예약 취소", description = "사용자가 자신의 예약을 취소합니다.")
    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<Void> cancelReservation(
            @Parameter(description = "예약 ID") @PathVariable Long reservationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getUserId();
        log.info("예약 취소 - 예약 ID: {}, 사용자 ID: {}", reservationId, userId);

        // 본인 예약 확인 및 취소 처리
        boothExperienceService.cancelUserReservation(reservationId, userId);
        return ResponseEntity.ok().build();
    }

    // 10. 대기열 상태 조회
    @Operation(summary = "대기열 상태 조회", description = "특정 체험의 현재 대기열 상태를 조회합니다.")
    @GetMapping("/{experienceId}/queue-status")
    public ResponseEntity<List<BoothExperienceReservationResponseDto>> getQueueStatus(
            @Parameter(description = "체험 ID") @PathVariable Long experienceId) {

        log.info("대기열 상태 조회 - 체험 ID: {}", experienceId);
        List<BoothExperienceReservationResponseDto> reservations = boothExperienceService
                .getExperienceReservations(experienceId);

        // 대기중이거나 진행중인 예약만 필터링
        List<BoothExperienceReservationResponseDto> activeReservations = reservations.stream()
                .filter(r -> "WAITING".equals(r.getStatusCode()) ||
                        "READY".equals(r.getStatusCode()) ||
                        "IN_PROGRESS".equals(r.getStatusCode()))
                .toList();

        return ResponseEntity.ok(activeReservations);
    }

    // 11. 디버그용 - 모든 체험 조회 (예약 활성화 여부 포함)
    @Operation(summary = "디버그용 모든 체험 조회", description = "예약 활성화 여부와 관계없이 모든 체험을 조회합니다.")
    @GetMapping("/debug/all")
    public ResponseEntity<List<BoothExperienceResponseDto>> getAllExperiencesForDebug() {
        log.info("디버그용 모든 체험 조회");
        List<BoothExperienceResponseDto> experiences = boothExperienceService.getAvailableExperiences(
                null, null, null, null, null, null, "startTime", "asc");
        return ResponseEntity.ok(experiences);
    }

    // 12. 권한별 관리 가능한 부스 목록 조회 (체험 등록용)
    @Operation(summary = "관리 가능한 부스 목록 조회", description = "사용자 권한에 따라 관리 가능한 부스 목록을 조회합니다.")
    @GetMapping("/manageable-booths")
    @FunctionAuth("getManageableBooths")
    public ResponseEntity<List<BoothResponseDto>> getManageableBooths(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("관리 가능한 부스 목록 조회 - 사용자 ID: {}, 권한: {}", 
                userDetails.getUserId(), userDetails.getRoleCode());
        
        List<BoothResponseDto> booths = boothExperienceService.getManageableBooths(
                userDetails.getUserId(), userDetails.getRoleCode());
        return ResponseEntity.ok(booths);
    }

    // 13. 권한별 관리 가능한 체험 목록 조회 (체험 관리용)
    @Operation(summary = "관리 가능한 체험 목록 조회", description = "사용자 권한에 따라 관리 가능한 모든 체험 목록을 조회합니다.")
    @GetMapping("/manageable-experiences")
    public ResponseEntity<List<BoothExperienceResponseDto>> getManageableExperiences(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("관리 가능한 체험 목록 조회 - 사용자 ID: {}, 권한: {}", 
                userDetails.getUserId(), userDetails.getRoleCode());
        
        List<BoothExperienceResponseDto> experiences = boothExperienceService.getManageableExperiences(
                userDetails.getUserId(), userDetails.getRoleCode());
        return ResponseEntity.ok(experiences);
    }

    // 14. 예약자 관리용 목록 조회 (필터링 지원)
    @Operation(summary = "예약자 관리 목록 조회", description = "행사/부스 담당자가 예약자를 관리하기 위한 목록을 조회합니다.")
    @GetMapping("/reservations/management")
    // @FunctionAuth("getReservationsForManagement") // 임시로 권한 체크 제거
    public ResponseEntity<Page<ReservationManagementResponseDto>> getReservationsForManagement(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "부스 ID 필터") @RequestParam(required = false) Long boothId,
            @Parameter(description = "예약자 이름 검색") @RequestParam(required = false) String reserverName,
            @Parameter(description = "예약자 전화번호 검색") @RequestParam(required = false) String reserverPhone,
            @Parameter(description = "체험일 (YYYY-MM-DD)") @RequestParam(required = false) String experienceDate,
            @Parameter(description = "체험 상태 필터") @RequestParam(required = false) String statusCode,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 기준") @RequestParam(defaultValue = "reservedAt") String sortBy,
            @Parameter(description = "정렬 방향") @RequestParam(defaultValue = "asc") String sortDirection) {

        log.info("예약자 관리 목록 조회 - 사용자 ID: {}, 권한: {}", 
                userDetails.getUserId(), userDetails.getRoleCode());

        ReservationManagementRequestDto requestDto = ReservationManagementRequestDto.builder()
                .boothId(boothId)
                .reserverName(reserverName)
                .reserverPhone(reserverPhone)
                .experienceDate(experienceDate)
                .statusCode(statusCode)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        Page<ReservationManagementResponseDto> reservations = boothExperienceService.getReservationsForManagement(
                userDetails.getUserId(), userDetails.getRoleCode(), requestDto);
        
        return ResponseEntity.ok(reservations);
    }

    // 15. 부스 체험 현황 요약 조회
    @Operation(summary = "부스 체험 현황 요약", description = "특정 체험의 현재 상황을 요약해서 조회합니다.")
    @GetMapping("/{experienceId}/summary")
    // @FunctionAuth("getExperienceSummary") // 임시로 권한 체크 제거
    public ResponseEntity<BoothExperienceSummaryDto> getExperienceSummary(
            @Parameter(description = "체험 ID") @PathVariable Long experienceId) {

        log.info("부스 체험 현황 요약 조회 - 체험 ID: {}", experienceId);
        BoothExperienceSummaryDto summary = boothExperienceService.getExperienceSummary(experienceId);
        return ResponseEntity.ok(summary);
    }

    // 16. 예약 관리용 부스 목록 조회
    @Operation(summary = "예약 관리용 부스 목록", description = "예약자 관리 화면에서 사용할 부스 목록을 조회합니다.")
    @GetMapping("/manageable-booths-for-reservation")
    // @FunctionAuth("getManageableBoothsForReservation") // 임시로 권한 체크 제거
    public ResponseEntity<List<BoothResponseDto>> getManageableBoothsForReservation(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("예약 관리용 부스 목록 조회 - 사용자 ID: {}, 권한: {}", 
                userDetails.getUserId(), userDetails.getRoleCode());

        List<BoothResponseDto> booths = boothExperienceService.getManageableBoothsForReservationManagement(
                userDetails.getUserId(), userDetails.getRoleCode());
        
        log.info("최종 반환 부스 목록 크기: {}", booths.size());
        return ResponseEntity.ok(booths);
    }

    // 17. 해당 사용자 가장 최근 예약 현황
    @GetMapping("/user/{eventId}/waiting-count")
    public ResponseEntity<BoothUserRecentlyWaitingCount> getUserRecentlyEventWaitingCount(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long eventId
    ){
        return ResponseEntity.ok(boothExperienceService
            .getUserRecentlyEventWaitingCount(userDetails,eventId));
    }
}