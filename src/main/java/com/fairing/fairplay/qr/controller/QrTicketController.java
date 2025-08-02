package com.fairing.fairplay.qr.controller;


import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketResponseDto;
import com.fairing.fairplay.qr.service.QrTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/qr-tickets")
public class QrTicketController {

  private final QrTicketService qrTicketService;

  // 마이페이지에서 QR 티켓 조회
  @GetMapping
  public ResponseEntity<QrTicketResponseDto> issueMember(@RequestBody QrTicketRequestDto dto) {
    return ResponseEntity.ok(qrTicketService.issueMember(dto));
  }

  // 비회원 QR 티켓 조회 (참석자)

  // QR 티켓 만료

  // QR 티켓 재발급

  // QR 티켓 삭제?

}
