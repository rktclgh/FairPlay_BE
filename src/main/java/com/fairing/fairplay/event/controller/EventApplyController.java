package com.fairing.fairplay.event.controller;

import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.dto.EventApplyProcessDto;
import com.fairing.fairplay.event.dto.EventApplyRequestDto;
import com.fairing.fairplay.event.dto.EventApplyResponseDto;
import com.fairing.fairplay.event.entity.EventApply;
import com.fairing.fairplay.event.service.EventApplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Slf4j
public class EventApplyController {

    private final EventApplyService eventApplyService;

    @PostMapping("/apply")
    public ResponseEntity<EventApplyResponseDto> submitEventApplication(
            @RequestBody EventApplyRequestDto requestDto) {

        EventApply eventApply = eventApplyService.submitEventApplication(requestDto);
        EventApplyResponseDto responseDto = EventApplyResponseDto.from(eventApply);

        return ResponseEntity.ok(responseDto);
    }

    // 신청 상태 확인 (이메일로 조회)
    @GetMapping("/apply/check")
    public ResponseEntity<EventApplyResponseDto> checkApplicationStatus(
            @RequestParam String eventEmail) {

        Optional<EventApply> eventApply = eventApplyService.findByEventEmail(eventEmail);

        if (eventApply.isPresent()) {
            EventApplyResponseDto responseDto = EventApplyResponseDto.from(eventApply.get());
            return ResponseEntity.ok(responseDto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 행사 신청 목록 조회 (관리자용)
    @GetMapping("/applications")
    @PreAuthorize("hasAuthority('ADMIN')")
    @FunctionAuth("getPendingApplications")
    public ResponseEntity<Page<EventApplyResponseDto>> getPendingApplications(
            @RequestParam(required = false) String status,
            Pageable pageable) {

        Page<EventApply> pendingApplications = eventApplyService.getApplications(status, pageable);
        Page<EventApplyResponseDto> responsePage = pendingApplications.map(EventApplyResponseDto::from);

        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<EventApplyResponseDto> getEventApplicationDetail(
            @PathVariable Long applicationId) {
        EventApplyResponseDto detail = eventApplyService.getEventApplicationDetail(applicationId);
        return ResponseEntity.ok(detail);
    }

    // 행사 신청 승인/반려
    @PutMapping("/applications/{applicationId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @FunctionAuth("processEventApplication")
    public ResponseEntity<String> processEventApplication(
            @PathVariable Long applicationId,
            @RequestBody EventApplyProcessDto processDto,
            @AuthenticationPrincipal CustomUserDetails auth) {

        if ("approve".equals(processDto.getAction())) {
            eventApplyService.approveEventApplication(applicationId, processDto.getAdminComment(), auth.getUserId());
            return ResponseEntity.ok("행사 신청이 승인되었습니다.");
        } else if ("reject".equals(processDto.getAction())) {
            eventApplyService.rejectEventApplication(applicationId, processDto.getAdminComment());
            return ResponseEntity.ok("행사 신청이 반려되었습니다.");
        } else {
            return ResponseEntity.badRequest().body("유효하지 않은 액션입니다.");
        }
    }
}