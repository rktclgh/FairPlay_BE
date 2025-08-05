package com.fairing.fairplay.qr.controller;


import com.fairing.fairplay.qr.dto.QrTicketReissueRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketReissueResponseDto;
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

  // 1. QR 티켓 재발급 - QR 티켓 조회 시 이미지코드와 수동 코드만 재발급(새로고침버튼)
  @PostMapping("/reissue")
  public ResponseEntity<QrTicketUpdateResponseDto> reissueQrTicket(
      @RequestBody QrTicketUpdateRequestDto dto) {
    return ResponseEntity.ok(qrTicketService.reissueQrTicket(dto));
  }

  // 2. QR 티켓 재발급 - 회원이 QR 티켓 분실 시 회원이 티켓 자체 재발급 요청
  /*
  * 마이 페이지 접근 가능한 경우
  * 1. "관리자 문의"
  * 2. 관리자 승인 후 발급
  * 3. 이메일로 발급 완료 메일 전송
  * */
//  @PostMapping("/admin/reissue")
//  public ResponseEntity<QrTicketReissueResponseDto> requestReissueQrTicket(@RequestBody QrTicketRequestDto dto) {
//    return ResponseEntity.ok(qrTicketService.reissueAdminQrTicket());
//  }


  // 3. QR 티켓 강제 재발급 - 비회원/회원 일부 QR 티켓 분실 시 이메일 재전송
  /*
   * 마이 페이지도 접근 안될 경우
   * 1. "관리자 문의"
   * 2. 관리자 승인 후 발급
   * 3. 이메일로 발급 완료 메일 전송
   * */
  @PostMapping("/admin/reissue/send-email")
  public ResponseEntity<QrTicketReissueResponseDto> reissueAdminQrTicket(@RequestBody QrTicketReissueRequestDto dto){
    return ResponseEntity.ok(qrTicketService.reissueAdminQrTicket(dto));
  }

  // QR 티켓 만료

  // QR 티켓 삭제?
}
