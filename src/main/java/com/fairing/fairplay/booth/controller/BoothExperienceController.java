package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.*;
import com.fairing.fairplay.booth.service.BoothExperienceService;
import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // 2. 부스 체험 목록 조회 (부스 담당자용)
    @Operation(summary = "부스 체험 목록 조회", description = "특정 부스의 모든 체험 목록을 조회합니다.")
    @GetMapping("/booths/{boothId}")
    @FunctionAuth("getBoothExperiences")
    public ResponseEntity<List<BoothExperienceResponseDto>> getBoothExperiences(
            @Parameter(description = "부스 ID") @PathVariable Long boothId) {

        log.info("부스 체험 목록 조회 - 부스 ID: {}", boothId);
        List<BoothExperienceResponseDto> experiences = boothExperienceService.getBoothExperiences(boothId);
        return ResponseEntity.ok(experiences);
    }

    // 3. 체험 가능한 부스 체험 목록 조회 (참여자용)
    @Operation(summary = "체험 가능한 부스 목록", description = "현재 예약 가능한 모든 부스 체험을 조회합니다.")
    @GetMapping("/available")
    public ResponseEntity<List<BoothExperienceResponseDto>> getAvailableExperiences() {

        log.info("체험 가능한 부스 목록 조회");
        List<BoothExperienceResponseDto> experiences = boothExperienceService.getAvailableExperiences();
        return ResponseEntity.ok(experiences);
    }

    // 4. 부스 체험 상세 조회
    /*
     * @Operation(summary = "부스 체험 상세 조회", description = "특정 체험의 상세 정보를 조회합니다.")
     * 
     * @GetMapping("/{experienceId}")
     * public ResponseEntity<BoothExperienceResponseDto> getBoothExperience(
     * 
     * @Parameter(description = "체험 ID") @PathVariable Long experienceId) {
     * 
     * log.info("부스 체험 상세 조회 - 체험 ID: {}", experienceId);
     * // 단일 조회 메서드 필요시 Service에 추가 구현 필요
     * return ResponseEntity.notImplemented().build();
     * }
     */

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
            @Parameter(description = "사용자 ID") @RequestParam Long userId) {

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
}