package com.fairing.fairplay.qr.service;

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
  public QrTicketResponseDto issueMember(QrTicketRequestDto dto) {
    return qrTicketManager.issueMemberTicket(dto);
  }

  // 비회원 QR 티켓 조회 -> QR 티켓 링크 통한 조회
  @Transactional
  public QrTicketResponseDto issueGuest(String token) {
    return qrTicketManager.issueGuestTicket(token);
  }

  // QR 티켓 재발급 - 새로고침 버튼
  @Transactional
  public QrTicketUpdateResponseDto reissueQrTicket(QrTicketUpdateRequestDto dto) {
    return qrTicketManager.reissueQrTicket(dto);
  }

  // 관리자 강제 QR 티켓 링크 재발급
  @Transactional
  public QrTicketReissueResponseDto reissueAdminQrTicket(QrTicketReissueRequestDto dto) {
    return qrTicketManager.reissueByAdmin(dto);
  }

  // 마이 페이지 강제 QR 티켓 재발급 - 행사 관리자
//  @Transactional
//  public QrTicketReissueResponseDto reissueAdminQrTicket(QrTicketUpdateRequestDto dto) {
//    // attendeeId 받음
//
//    // 참석자 ID 조회
//    Attendee attendee = attendeeRepository.findById(attendeeId);
//
//    // qr url 재발급
//    String qrUrl = qrLinkService.generateQrLink(dto);
//
//    // 메일 전송
//    qrEmailService.sendQrEmail(qrUrl, name, email);
//    // 메일 전송 성공 응답 - 메일 내용: 성공만 했다. url은 마이페이지에서 확인해라
//  }
}
