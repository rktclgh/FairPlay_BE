package com.fairing.fairplay.event.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.dto.EventVersionComparisonDto;
import com.fairing.fairplay.event.dto.EventVersionResponseDto;
import com.fairing.fairplay.event.entity.EventVersion;
import com.fairing.fairplay.event.service.EventVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/versions")
@RequiredArgsConstructor
@Slf4j
public class EventVersionController {

    private final EventVersionService eventVersionService;

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Page<EventVersionResponseDto>> getEventVersions(
            @PathVariable Long eventId,
            Pageable pageable) {

        Page<EventVersion> versionPage = eventVersionService.getEventVersions(eventId, pageable);
        Page<EventVersionResponseDto> responseDto = versionPage.map(EventVersionResponseDto::from);

        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/{versionNumber}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<EventVersionResponseDto> getEventVersion(
            @PathVariable Long eventId,
            @PathVariable Integer versionNumber) {

        EventVersion eventVersion = eventVersionService.getEventVersion(eventId, versionNumber);
        EventVersionResponseDto responseDto = EventVersionResponseDto.from(eventVersion);

        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/{versionNumber}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> restoreToVersion(
            @PathVariable Long eventId,
            @PathVariable Integer versionNumber,
            @AuthenticationPrincipal CustomUserDetails auth) {

        eventVersionService.restoreToVersion(eventId, versionNumber, auth.getUserId());
        
        return ResponseEntity.ok("버전 " + versionNumber + "로 복구가 완료되었습니다.");
    }

    @GetMapping("/compare")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<EventVersionComparisonDto> compareVersions(
            @PathVariable Long eventId,
            @RequestParam Integer version1,
            @RequestParam Integer version2) {

        EventVersionComparisonDto comparisonDto = eventVersionService.compareVersions(eventId, version1, version2);

        return ResponseEntity.ok(comparisonDto);
    }
}