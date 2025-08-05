package com.fairing.fairplay.qr.controller;


import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketResponseDto;
import com.fairing.fairplay.qr.dto.QrTicketUpdateRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketUpdateResponseDto;
import com.fairing.fairplay.qr.service.QrTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/qr-tickets")
public class QrTicketController {

  private final QrTicketService qrTicketService;

  // 마이페이지에서 QR 티켓 조회
  @PostMapping
  public ResponseEntity<QrTicketResponseDto> issueMember(@RequestBody QrTicketRequestDto dto) {
    return ResponseEntity.ok(qrTicketService.issueMember(dto));
  }

  // 비회원 QR 티켓 조회 (참석자)
  @GetMapping("/{token}")
  public ResponseEntity<QrTicketResponseDto> issueGuest(@PathVariable String token) {
    return ResponseEntity.ok(qrTicketService.issueGuest(token));
  }

  // QR 티켓 재발급
  @PostMapping("/reissue")
  public ResponseEntity<QrTicketUpdateResponseDto> reissueQrTicket(
      @RequestBody QrTicketUpdateRequestDto dto) {
    return ResponseEntity.ok(qrTicketService.reissueQrTicket(dto));
  }

  // QR 티켓 만료

  // QR 티켓 삭제?
}
