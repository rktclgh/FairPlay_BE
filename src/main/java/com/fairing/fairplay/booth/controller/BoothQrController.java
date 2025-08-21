package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.BoothEntryRequestDto;
import com.fairing.fairplay.booth.dto.BoothEntryResponseDto;
import com.fairing.fairplay.booth.service.BoothQrService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.qr.dto.scan.CheckResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/booth-qr")
public class BoothQrController {

  private final BoothQrService boothQrService;

  @PostMapping
  public ResponseEntity<BoothEntryResponseDto> boothEntry(@RequestBody BoothEntryRequestDto dto) {
    return ResponseEntity.status(HttpStatus.OK).body(boothQrService.checkIn(dto));
  }
}
