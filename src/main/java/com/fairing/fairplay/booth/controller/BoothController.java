package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.BoothUpdateRequestDto;
import com.fairing.fairplay.booth.service.BoothService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booths")
@RequiredArgsConstructor
public class BoothController {

    private final BoothService boothService;

    @PutMapping("/{boothId}")
    public ResponseEntity<Void> updateBooth(@PathVariable Long boothId, @RequestBody BoothUpdateRequestDto dto) {
        boothService.updateBooth(boothId, dto);
        return ResponseEntity.ok().build();
    }
}
