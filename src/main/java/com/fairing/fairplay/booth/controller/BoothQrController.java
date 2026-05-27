package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.BoothEntryRequestDto;
import com.fairing.fairplay.booth.dto.BoothEntryResponseDto;
import com.fairing.fairplay.booth.service.BoothQrService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
  @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('EVENT_MANAGER') or hasAuthority('BOOTH_MANAGER')")
  public ResponseEntity<BoothEntryResponseDto> boothEntry(
      @RequestBody BoothEntryRequestDto dto,
      @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.status(HttpStatus.OK).body(
        boothQrService.checkIn(dto, userDetails.getUserId(), userDetails.getRoleCode()));
  }
}
