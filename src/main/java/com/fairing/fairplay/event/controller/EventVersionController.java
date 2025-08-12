package com.fairing.fairplay.event.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.event.dto.EventDetailModificationResponseDto;
import com.fairing.fairplay.event.dto.EventVersionComparisonDto;
import com.fairing.fairplay.event.dto.EventVersionResponseDto;
import com.fairing.fairplay.event.entity.EventDetailModificationRequest;
import com.fairing.fairplay.event.entity.EventVersion;
import com.fairing.fairplay.event.service.EventDetailModificationRequestService;
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
    private final EventDetailModificationRequestService modificationRequestService;

    @GetMapping
    @PreAuthorize("hasAuthority('EVENT_MANAGER') or hasAuthority('ADMIN')")
    public ResponseEntity<Page<EventVersionResponseDto>> getEventVersions(
            @PathVariable Long eventId,
            Pageable pageable) {

        Page<EventVersion> versionPage = eventVersionService.getEventVersions(eventId, pageable);
        Page<EventVersionResponseDto> responseDto = versionPage.map(EventVersionResponseDto::from);

        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/{versionNumber}")
    @PreAuthorize("hasAuthority('EVENT_MANAGER') or hasAuthority('ADMIN')")
    public ResponseEntity<EventVersionResponseDto> getEventVersion(
            @PathVariable Long eventId,
            @PathVariable Integer versionNumber) {

        EventVersion eventVersion = eventVersionService.getEventVersion(eventId, versionNumber);
        EventVersionResponseDto responseDto = EventVersionResponseDto.from(eventVersion);

        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/{versionNumber}/restore-request")
    @PreAuthorize("hasAuthority('EVENT_MANAGER') or hasAuthority('ADMIN')")
    public ResponseEntity<EventDetailModificationResponseDto> createVersionRestoreRequest(
            @PathVariable Long eventId,
            @PathVariable Integer versionNumber,
            @AuthenticationPrincipal CustomUserDetails auth) {

        EventDetailModificationRequest request = modificationRequestService.createVersionRestoreRequest(
                eventId, versionNumber, auth.getUserId());
        
        EventDetailModificationResponseDto responseDto = EventDetailModificationResponseDto.from(request);
        
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/compare")
    @PreAuthorize("hasAuthority('EVENT_MANAGER') or hasAuthority('ADMIN')")
    public ResponseEntity<EventVersionComparisonDto> compareVersions(
            @PathVariable Long eventId,
            @RequestParam Integer version1,
            @RequestParam Integer version2) {

        EventVersionComparisonDto comparisonDto = eventVersionService.compareVersions(eventId, version1, version2);

        return ResponseEntity.ok(comparisonDto);
    }
}
