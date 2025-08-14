package com.fairing.fairplay.shareticket.controller;

import com.fairing.fairplay.shareticket.dto.ShareTicketInfoResponseDto;
import com.fairing.fairplay.shareticket.dto.ShareTicketSaveRequestDto;
import com.fairing.fairplay.shareticket.service.ShareTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

  // 공유폼생성
//  @PostMapping
//  public ResponseEntity<String> generateToken(@RequestBody ShareTicketSaveRequestDto dto) {
//    log.info("Generating token for share ticket:{}",dto.getTotalAllowed());
//    return ResponseEntity.ok(shareTicketService.generateToken(dto));
//  }

  // 공유폼조회
  // 폼링크 조회 시 기본 정보 세팅
  @GetMapping()
  public ResponseEntity<ShareTicketInfoResponseDto> getFormInfo(@RequestParam String token){
    return ResponseEntity.status(HttpStatus.OK).body(shareTicketService.getFormInfo(token));
  }

  //자동저장
}
