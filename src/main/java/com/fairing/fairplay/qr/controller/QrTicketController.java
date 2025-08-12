package com.fairing.fairplay.qr.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.qr.dto.QrTicketReissueGuestRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketReissueMemberRequestDto;
import com.fairing.fairplay.qr.dto.scan.AdminForceCheckRequestDto;
import com.fairing.fairplay.qr.dto.scan.CheckResponseDto;
import com.fairing.fairplay.qr.dto.scan.ManualCheckRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketReissueRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketReissueResponseDto;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketResponseDto;
import com.fairing.fairplay.qr.dto.QrTicketUpdateResponseDto;
import com.fairing.fairplay.qr.dto.scan.QrCheckRequestDto;
import com.fairing.fairplay.qr.service.EntryExitService;
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
  private final EntryExitService entryExitService;

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
  // QR 티켓 재발급 1-1 : QR 티켓 링크에서 재발급
  @PostMapping("/reissue/quest")
  public ResponseEntity<QrTicketUpdateResponseDto> reissueQrTicketByGuest(
      @RequestBody QrTicketReissueGuestRequestDto dto) {
    return ResponseEntity.ok(qrTicketService.reissueQrTicketByGuest(dto));
  }

  // QR 티켓 재발급 1-2 : 마이페이지에서 QR 재발급
  @PostMapping("/reissue/member")
  public ResponseEntity<QrTicketUpdateResponseDto> reissueQrTicketByMember(
      @RequestBody QrTicketReissueMemberRequestDto dto, @AuthenticationPrincipal CustomUserDetails userDetails) {
    return ResponseEntity.ok(qrTicketService.reissueQrTicketByMember(dto,userDetails));
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

  // QR 체크인
  @PostMapping("/check-in/qr")
  public ResponseEntity<CheckResponseDto> checkInWithQr(@RequestBody QrCheckRequestDto dto) {
    return ResponseEntity.ok(entryExitService.checkInWithQr(dto));

  }

  // 수동코드 체크인
  @PostMapping("/check-in/manual")
  public ResponseEntity<CheckResponseDto> checkInWithManual(@RequestBody ManualCheckRequestDto dto) {
    return ResponseEntity.ok(entryExitService.checkInWithManual(dto));

  }

  // QR 체크아웃
  @PostMapping("/check-out/qr")
  public ResponseEntity<CheckResponseDto> checkOutWithQr(@RequestBody QrCheckRequestDto dto) {
    return ResponseEntity.ok(entryExitService.checkOutWithQr(dto));

  }

  // 수동코드 체크아웃
  @PostMapping("/check-out/manual")
  public ResponseEntity<CheckResponseDto> checkOutWithManual(@RequestBody ManualCheckRequestDto dto) {
    return ResponseEntity.ok(entryExitService.checkOutWithManual(dto));

  }

  // 관리자 강제 입퇴장
  @PostMapping("/admin/check")
  public ResponseEntity<CheckResponseDto> adminForceCheck(
      @RequestBody AdminForceCheckRequestDto dto) {
    return ResponseEntity.ok(entryExitService.adminForceCheck(dto));
  }
}
