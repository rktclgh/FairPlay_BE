package com.fairing.fairplay.qr.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.qr.dto.CheckInResponseDto;
import com.fairing.fairplay.qr.dto.GuestManualCheckInRequestDto;
import com.fairing.fairplay.qr.dto.GuestQrCheckInRequestDto;
import com.fairing.fairplay.qr.dto.MemberManualCheckInRequestDto;
import com.fairing.fairplay.qr.dto.MemberQrCheckInRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketReissueRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketReissueResponseDto;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketResponseDto;
import com.fairing.fairplay.qr.dto.QrTicketUpdateRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketUpdateResponseDto;
import com.fairing.fairplay.qr.service.QrTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
  public ResponseEntity<QrTicketResponseDto> issueMember(@RequestBody QrTicketRequestDto dto,
      @AuthenticationPrincipal
      CustomUserDetails userDetails) {
    return ResponseEntity.ok(qrTicketService.issueMember(dto, userDetails));
  }

  // 비회원 QR 티켓 조회 (참석자)
  @GetMapping("/{token}")
  public ResponseEntity<QrTicketResponseDto> issueGuest(@PathVariable String token) {
    return ResponseEntity.ok(qrTicketService.issueGuest(token));
  }

  /*
   * 재발급
   * 1. 사용자가 새로고침 버튼 클릭해 QR 코드 재생성
   * 2. 회원이 마이페이지에서 QR 링크 조회 안될 때 관리자 강제 QR 티켓 리셋
   * 3. 마이페이지 접근 안되는 회원/비회원에게 강제 QR 티켓 링크 재발급해 메일 전송
   * */
  // QR 티켓 재발급 1
  @PostMapping("/reissue")
  public ResponseEntity<QrTicketUpdateResponseDto> reissueQrTicket(
      @RequestBody QrTicketUpdateRequestDto dto) {
    return ResponseEntity.ok(qrTicketService.reissueQrTicket(dto));
  }

  // QR 티켓 재발급 2
  @PostMapping("/admin/reissue")
  public ResponseEntity<QrTicketReissueResponseDto> reissueAdminQrTicketByUser(
      @RequestBody QrTicketReissueRequestDto dto) {
    return ResponseEntity.ok(qrTicketService.reissueAdminQrTicketByUser(dto));
  }


  // QR 티켓 재발급 3
  @PostMapping("/admin/reissue/send-email")
  public ResponseEntity<QrTicketReissueResponseDto> reissueAdminQrTicket(
      @RequestBody QrTicketReissueRequestDto dto) {
    return ResponseEntity.ok(qrTicketService.reissueAdminQrTicket(dto));
  }

  /*
   * 체크인
   * 1. 회원+QR
   * 2. 회원+수동
   * 3. 비회원+QR
   * 4. 비회원+수동
   *
   */

  // 체크인 1
  @PostMapping("/check-in/member/qr")
  public ResponseEntity<CheckInResponseDto> checkInWithQrByMember(@RequestBody
  MemberQrCheckInRequestDto dto, @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(qrTicketService.checkInWithQrByMember(dto));
  }

  // 체크인 2
  @PostMapping("/check-in/member/manual")
  public ResponseEntity<CheckInResponseDto> checkInWithManualByMember(@RequestBody
  MemberManualCheckInRequestDto dto, @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(qrTicketService.checkInWithManualByMember(dto));
  }

  // 체크인 3
  @PostMapping("/check-in/guest/qr")
  public ResponseEntity<CheckInResponseDto> checkInWithQrByGuest(@RequestBody
  GuestQrCheckInRequestDto dto) {
    return ResponseEntity.ok(qrTicketService.checkInWithQrByGuest(dto));
  }

  // 체크인 4
  @PostMapping("/check-in/guest/qr")
  public ResponseEntity<CheckInResponseDto> checkInWithManualByGuest(@RequestBody
  GuestManualCheckInRequestDto dto) {
    return ResponseEntity.ok(qrTicketService.checkInWithManualByGuest(dto));
  }
}
