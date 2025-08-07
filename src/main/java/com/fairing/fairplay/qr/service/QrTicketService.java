package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.qr.dto.QrTicketReissueRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketReissueResponseDto;
import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketResponseDto;
import com.fairing.fairplay.qr.dto.QrTicketUpdateRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketUpdateResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*QR티켓 서비스*/
@Service
@RequiredArgsConstructor
@Slf4j
public class QrTicketService {

  private final QrTicketManager qrTicketManager;

  // 회원 QR 티켓 조회 -> 마이페이지에서 조회
  @Transactional
  public QrTicketResponseDto issueMember(QrTicketRequestDto dto, CustomUserDetails userDetails) {
    return qrTicketManager.issueMemberTicket(dto, userDetails);
  }

  // 비회원 QR 티켓 조회 -> QR 티켓 링크 통한 조회
  @Transactional
  public QrTicketResponseDto issueGuest(String token) {
    return qrTicketManager.issueGuestTicket(token);
  }

  /*
   * 재발급
   * 1. 사용자가 새로고침 버튼 클릭해 QR 코드 재생성
   * 2. 회원이 마이페이지에서 QR 링크 조회 안될 때 관리자 강제 QR 티켓 리셋
   * 3. 마이페이지 접근 안되는 회원/비회원에게 강제 QR 티켓 링크 재발급해 메일 전송
   * */
  // QR 티켓 재발급 1
  @Transactional
  public QrTicketUpdateResponseDto reissueQrTicket(QrTicketUpdateRequestDto dto) {
    return qrTicketManager.reissueQrTicket(dto);
  }

  // QR 티켓 재발급 2
  @Transactional
  public QrTicketReissueResponseDto reissueAdminQrTicketByUser(QrTicketReissueRequestDto dto) {
    return qrTicketManager.reissueAdminQrTicketByUser(dto);
  }

  // QR 티켓 재발급 3
  @Transactional
  public QrTicketReissueResponseDto reissueAdminQrTicket(QrTicketReissueRequestDto dto) {
    return qrTicketManager.reissueAdminQrTicket(dto);
  }
}
