package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.*;
import com.fairing.fairplay.booth.entity.Booth;
import com.fairing.fairplay.booth.repository.BoothRepository;
import com.fairing.fairplay.booth.service.BoothService;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.user.dto.BoothAdminRequestDto;
import com.fairing.fairplay.user.dto.BoothAdminResponseDto;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BoothController {

    private final BoothService boothService;
    private final EventRepository eventRepository;
    private final BoothRepository boothRepository;
    private final UserRepository userRepository;

    @GetMapping("/api/events/{eventId}/booths/host")
    @PreAuthorize("hasAuthority('EVENT_MANAGER')")
    @FunctionAuth("getAllBooths")
    public ResponseEntity<List<BoothSummaryForManagerResponseDto>> getAllBooths(@PathVariable Long eventId, @AuthenticationPrincipal CustomUserDetails auth) {
        log.info("관리자용 행사 조회 시도");
        checkEventManager(eventId, auth);
        log.info("관리자용 행사 조회 완료");

        return ResponseEntity.ok(boothService.getAllBooths(eventId));
    }

    @GetMapping("/api/events/{eventId}/booths")
    public ResponseEntity<List<BoothSummaryResponseDto>> getBooths(@PathVariable Long eventId) {
        log.info("부스 조회 시도");
        List<BoothSummaryResponseDto> response = boothService.getBooths(eventId);
        log.info("조회 성공: {}", response);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/events/{eventId}/booths/{boothId}")
    public ResponseEntity<BoothDetailResponseDto> getBoothDetails(@PathVariable Long eventId, @PathVariable Long boothId) {
        log.info("부스 상세 정보 조회 시도: boothId={}", boothId);
        return ResponseEntity.ok(boothService.getBoothDetails(boothId));
    }

    @PatchMapping("/api/events/{eventId}/booths/{boothId}")
    @PreAuthorize("hasAuthority('EVENT_MANAGER') or hasAuthority('BOOTH_MANAGER')")
    @FunctionAuth("updateBooth")
    public ResponseEntity<BoothDetailResponseDto> updateBooth(@PathVariable Long eventId, @PathVariable Long boothId, @RequestBody BoothUpdateRequestDto dto, @AuthenticationPrincipal CustomUserDetails auth) {
        if (auth.getRoleCode().equals("EVENT_MANAGER")) {
            checkEventManager(eventId, auth);
        } else if (auth.getRoleCode().equals("BOOTH_MANAGER")) {
            checkBoothManager(boothId, auth);
        } else {
            throw new CustomException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }
        return ResponseEntity.ok(boothService.updateBooth(boothId, dto));
    }

    @DeleteMapping("/api/events/{eventId}/booths/{boothId}")
    @PreAuthorize("hasAuthority('EVENT_MANAGER')")
    @FunctionAuth("deleteBooth")
    public ResponseEntity<String> deleteBooth(@PathVariable Long eventId, @PathVariable Long boothId, @AuthenticationPrincipal CustomUserDetails auth) {
        checkEventManager(eventId, auth);

        boothService.deleteBooth(boothId);
        return ResponseEntity.ok("부스 삭제 완료(soft delete)");
    }

    @PatchMapping("/api/events/{eventId}/booths/{boothId}/manager")
    @PreAuthorize("hasAuthority('EVENT_MANAGER') or hasAuthority('BOOTH_MANAGER')")
    @FunctionAuth("updateBoothAdminInfo")
    public ResponseEntity<BoothAdminResponseDto> updateBoothAdminInfo (
            @PathVariable Long eventId,
            @PathVariable Long boothId,
            @AuthenticationPrincipal CustomUserDetails auth,
            @RequestBody BoothAdminRequestDto dto
            ) {
        if (auth.getRoleCode().equals("EVENT_MANAGER")) {
            checkEventManager(eventId, auth);
        } else if (auth.getRoleCode().equals("BOOTH_MANAGER")) {
            checkBoothManager(boothId, auth);
        } else {
            throw new CustomException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        BoothAdminResponseDto responseDto = boothService.updateBoothAdmin(boothId, dto);
        return ResponseEntity.ok(responseDto);
    }

    /********************** 부스 타입 관리 **********************/
    @PostMapping("/api/events/{eventId}/booths/types")
    @PreAuthorize("hasAuthority('EVENT_MANAGER')")
    @FunctionAuth("createBoothType")
    public ResponseEntity<BoothTypeDto> createBoothType(@PathVariable Long eventId, @RequestBody BoothTypeDto dto, @AuthenticationPrincipal CustomUserDetails auth) {
        checkEventManager(eventId, auth);

        BoothTypeDto response = boothService.createBoothType(eventId, dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/events/{eventId}/booths/types")
    public ResponseEntity<List<BoothTypeDto>> getBoothTypes(@PathVariable Long eventId) {
        List<BoothTypeDto> response = boothService.getBoothTypes(eventId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api/events/{eventId}/booths/types/{boothTypeId}")
    @PreAuthorize("hasAuthority('EVENT_MANAGER')")
    @FunctionAuth("updateBoothType")
    public ResponseEntity<BoothTypeDto> updateBoothType(@PathVariable Long eventId, @PathVariable Long boothTypeId, @RequestBody BoothTypeDto dto, @AuthenticationPrincipal CustomUserDetails auth) {
        checkEventManager(eventId, auth);

        BoothTypeDto response = boothService.updateBoothType(boothTypeId, dto);
        return ResponseEntity.ok(response);
    }

    // 부스 타입 삭제  (소프트 딜리트)
    @DeleteMapping("/api/events/{eventId}/booths/types/{boothTypeId}")
    @PreAuthorize("hasAuthority('EVENT_MANAGER')")
    @FunctionAuth("deleteBoothType")
    public ResponseEntity<String> deleteBoothType(@PathVariable Long eventId, @PathVariable Long boothTypeId, @AuthenticationPrincipal CustomUserDetails auth) {
        checkEventManager(eventId, auth);

        boothService.deleteBoothType(boothTypeId);
        return ResponseEntity.ok("삭제 완료");
    }

    /********************** 부스 관리자 기능 **********************/
    // 부스 관리자 - 내 부스 목록 조회 (실제 부스)
    @GetMapping("/api/booths/my-booths")
    @PreAuthorize("hasAuthority('BOOTH_MANAGER')")
    @FunctionAuth("getMyBooths")
    public ResponseEntity<List<BoothAdminDashboardDto>> getMyBooths(
            @AuthenticationPrincipal CustomUserDetails user) {
        
        List<BoothAdminDashboardDto> booths = boothService.getMyBooths(user.getUserId());
        return ResponseEntity.ok(booths);
    }

    /***** 헬퍼 메소드 *****/
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

}
