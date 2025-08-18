package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.BoothCancelPageDto;
import com.fairing.fairplay.booth.dto.BoothCancelRequestDto;
import com.fairing.fairplay.booth.service.BoothCancelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booths/cancel")
@RequiredArgsConstructor
public class BoothCancelController {

    private final BoothCancelService boothCancelService;

    // 부스 취소 페이지 정보 조회 (이메일 링크에서 접근)
    @GetMapping("/{applicationId}")
    public ResponseEntity<BoothCancelPageDto> getBoothCancelInfo(@PathVariable Long applicationId) {
        BoothCancelPageDto cancelInfo = boothCancelService.getBoothCancelInfo(applicationId);
        return ResponseEntity.ok(cancelInfo);
    }

    // 부스 취소 요청 처리
    @PostMapping("/{applicationId}")
    public ResponseEntity<Void> requestBoothCancel(
            @PathVariable Long applicationId,
            @RequestBody BoothCancelRequestDto cancelRequest) {
        boothCancelService.requestBoothCancel(applicationId, cancelRequest);
        return ResponseEntity.ok().build();
    }
}