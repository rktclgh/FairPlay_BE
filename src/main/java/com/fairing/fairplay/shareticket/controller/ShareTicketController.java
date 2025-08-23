package com.fairing.fairplay.shareticket.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.shareticket.dto.ShareTicketInfoResponseDto;
import com.fairing.fairplay.shareticket.dto.ShareTicketSaveRequestDto;
import com.fairing.fairplay.shareticket.dto.ShareTicketSaveResponseDto;
import com.fairing.fairplay.shareticket.dto.TokenResponseDto;
import com.fairing.fairplay.shareticket.service.ShareTicketAttendeeService;
import com.fairing.fairplay.shareticket.service.ShareTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 테스트용
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/form")
public class ShareTicketController {

  private final ShareTicketService shareTicketService;
  private final ShareTicketAttendeeService shareTicketAttendeeService;

  // 공유 폼 링크 및 참석자 생성
//  @PostMapping
//  public ResponseEntity<ShareTicketSaveResponseDto> saveShareTicketAndAttendee(
//      @RequestBody ShareTicketSaveRequestDto dto,
//      @AuthenticationPrincipal CustomUserDetails userDetails) {
//    return ResponseEntity.ok(
//        shareTicketAttendeeService.saveShareTicketAndAttendee(userDetails, dto));
//  }

  // 공유폼조회
  // 폼링크 조회 시 기본 정보 세팅
  @GetMapping
  public ResponseEntity<ShareTicketInfoResponseDto> getFormInfo(@RequestParam String token) {
    return ResponseEntity.status(HttpStatus.OK).body(shareTicketService.getFormInfo(token));
  }

  @GetMapping("{reservationId}")
  public ResponseEntity<TokenResponseDto> getFormLink(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PathVariable Long reservationId) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(shareTicketService.getFormLink(userDetails, reservationId));
  }
}
