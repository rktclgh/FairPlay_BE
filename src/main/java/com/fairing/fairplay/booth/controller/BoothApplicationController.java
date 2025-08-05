package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.*;
import com.fairing.fairplay.booth.service.BoothApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/booth/applications")
@RequiredArgsConstructor
public class BoothApplicationController {

    private final BoothApplicationService boothApplicationService;

    // 1. 고객 부스 신청
    @PostMapping
    public ResponseEntity<Long> apply(@RequestBody BoothApplicationRequestDto dto) {
        Long id = boothApplicationService.applyBooth(dto);
        return ResponseEntity.created(URI.create("/api/booth/applications/" + id)).body(id);
    }

    // 2. 관리자 - 신청 목록 조회
    @GetMapping
    public ResponseEntity<List<BoothApplicationListDto>> getList(@RequestParam Long eventId) {
        List<BoothApplicationListDto> list = boothApplicationService.getBoothApplications(eventId);
        return ResponseEntity.ok(list);
    }

    // 3. 관리자 - 신청 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<BoothApplicationResponseDto> getDetail(@PathVariable Long id) {
        BoothApplicationResponseDto dto = boothApplicationService.getBoothApplication(id);
        return ResponseEntity.ok(dto);
    }

    // 4. 관리자 - 승인/반려 처리
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id,
                                             @RequestBody BoothApplicationStatusUpdateDto dto) {
        boothApplicationService.updateStatus(id, dto);
        return ResponseEntity.ok().build();
    }
}
