package com.fairing.fairplay.creator.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.creator.dto.CreatorRequestDto;
import com.fairing.fairplay.creator.dto.CreatorResponseDto;
import com.fairing.fairplay.creator.service.CreatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 제작자 관리 API
 * 일반 사용자: 조회만 가능
 * 전체 관리자: CRUD 모두 가능
 */
@RestController
@RequestMapping("/api/creators")
@RequiredArgsConstructor
@Slf4j
public class CreatorController {

    private final CreatorService creatorService;

    /**
     * 제작자 목록 조회 (일반 사용자용 - 활성화된 것만)
     */
    @GetMapping
    public ResponseEntity<List<CreatorResponseDto>> getActiveCreators() {
        List<CreatorResponseDto> creators = creatorService.getActiveCreators();
        return ResponseEntity.ok(creators);
    }

    /**
     * 제작자 목록 조회 (관리자용 - 비활성화 포함)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<CreatorResponseDto>> getAllCreators(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("관리자 제작자 목록 조회 - userId: {}", userDetails.getUserId());
        List<CreatorResponseDto> creators = creatorService.getAllCreators();
        return ResponseEntity.ok(creators);
    }

    /**
     * 특정 제작자 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<CreatorResponseDto> getCreatorById(@PathVariable Long id) {
        CreatorResponseDto creator = creatorService.getCreatorById(id);
        return ResponseEntity.ok(creator);
    }

    /**
     * 제작자 생성 (전체 관리자만)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<CreatorResponseDto> createCreator(
            @RequestBody CreatorRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("제작자 생성 요청 - userId: {}, name: {}", userDetails.getUserId(), requestDto.getName());
        CreatorResponseDto created = creatorService.createCreator(requestDto);
        return ResponseEntity.ok(created);
    }

    /**
     * 제작자 수정 (전체 관리자만)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<CreatorResponseDto> updateCreator(
            @PathVariable Long id,
            @RequestBody CreatorRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("제작자 수정 요청 - userId: {}, creatorId: {}", userDetails.getUserId(), id);
        CreatorResponseDto updated = creatorService.updateCreator(id, requestDto);
        return ResponseEntity.ok(updated);
    }

    /**
     * 제작자 삭제 (전체 관리자만)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteCreator(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("제작자 삭제 요청 - userId: {}, creatorId: {}", userDetails.getUserId(), id);
        creatorService.deleteCreator(id);
        return ResponseEntity.noContent().build();
    }
}
