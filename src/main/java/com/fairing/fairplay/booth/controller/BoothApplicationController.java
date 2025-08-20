package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.*;
import com.fairing.fairplay.booth.entity.Booth;
import com.fairing.fairplay.booth.repository.BoothRepository;
import com.fairing.fairplay.booth.service.BoothApplicationService;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/booths/apply")
@RequiredArgsConstructor
@Slf4j
public class BoothApplicationController {

    private final BoothApplicationService boothApplicationService;
    private final EventRepository eventRepository;
    private final BoothRepository boothRepository;

    // 공통 권한 체크 메서드
    private void checkEventManager(Long eventId, CustomUserDetails user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 행사를 찾을 수 없습니다."));
        Long managerId = event.getManager().getUserId();
        if (!managerId.equals(user.getUserId())) {
            log.info("담당 행사 관리자가 아님: managerId={}, userId={}",
                    managerId, user.getUserId());
            throw new CustomException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }
    }

    private void checkBoothManager(Long boothId, CustomUserDetails user) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 부스를 찾을 수 없습니다."));
        Long managerId = booth.getBoothAdmin().getUserId();
        if (!managerId.equals(user.getUserId())) {
            log.info("담당 부스 관리자가 아님: managerId={}, userId={}",
                    managerId, user.getUserId());
            throw new CustomException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }
    }

    // 1. 고객 부스 신청 (권한 필요 없음)
    @PostMapping
    public ResponseEntity<BoothApplicationResponseDto> apply(@PathVariable Long eventId, @RequestBody BoothApplicationRequestDto dto) {
        BoothApplicationResponseDto responseDto = boothApplicationService.applyBooth(eventId, dto);
        return ResponseEntity.ok(responseDto);
    }

    // 2. 관리자 - 신청 목록 조회
    @FunctionAuth("getList")
    @GetMapping
    public ResponseEntity<List<BoothApplicationListDto>> getList(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long eventId) {

        checkEventManager(eventId, user);
        List<BoothApplicationListDto> list = boothApplicationService.getBoothApplications(eventId);
        return ResponseEntity.ok(list);
    }

    // 3. 관리자 - 신청 상세 조회
    @GetMapping("/{applicationId}")
    @FunctionAuth("getDetail")
    public ResponseEntity<BoothApplicationResponseDto> getDetail(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long eventId,
            @PathVariable Long applicationId) {
        checkEventManager(eventId, user);

        BoothApplicationResponseDto dto = boothApplicationService.getBoothApplication(applicationId);
        return ResponseEntity.ok(dto);
    }

    // 4. 관리자 - 승인/반려 처리
    @PutMapping("/{applicationId}/status")
    @FunctionAuth("updateStatus")
    public ResponseEntity<Void> updateStatus(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long eventId,
            @PathVariable Long applicationId,
            @RequestBody BoothApplicationStatusUpdateDto dto) {

        checkEventManager(eventId, user);
        boothApplicationService.updateStatus(applicationId, dto);
        return ResponseEntity.ok().build();
    }

    // 5. 행사 관리자 - 결제 상태 변경 처리
    @PutMapping("/{applicationId}/payment-status")
    @FunctionAuth("updatePaymentStatus")
    public ResponseEntity<Void> updatePaymentStatus(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long eventId,
            @PathVariable Long applicationId,
            @RequestBody BoothPaymentStatusUpdateDto dto) {

        checkEventManager(eventId, user);
        boothApplicationService.updatePaymentStatus(applicationId, dto);
        return ResponseEntity.ok().build();
    }

    // 6. 부스 취소 요청
    @PutMapping("/{applicationId}/cancel")
    @FunctionAuth("cancelApplication")
    public ResponseEntity<Void> cancelApplication(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long eventId,
            @PathVariable Long applicationId) {

        boothApplicationService.cancelApplication(applicationId, user.getUserId());
        return ResponseEntity.ok().build();
    }

}
