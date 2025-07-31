package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.qr.dto.QrTicketRequestDto;
import com.fairing.fairplay.qr.dto.QrTicketResponseDto;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import com.fairing.fairplay.qr.util.CodeGenerator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*QR티켓 서비스*/
@Service
@RequiredArgsConstructor
public class QrTicketService {

  private final QrTicketRepository qrTicketRepository;
  private final CodeGenerator codeGenerator;
  private final QrTicketInitProvider qrTicketInitProvider;

  // 회원 QR 티켓 조회 -> 마이페이지에서 조회
  @Transactional
  public QrTicketResponseDto issueMember(QrTicketRequestDto dto) {
    // QR 티켓 조회해 qr code, manualcode 생성해서 반환
    QrTicket qrTicket = qrTicketInitProvider.load(dto, 1);
    qrTicket.setQrCode(codeGenerator.generateRandomToken());
    qrTicket.setManualCode(codeGenerator.generateManualCode());
    return new QrTicketResponseDto();
  }

  // 비회원 QR 티켓 조회 -> QR 티켓 링크 통한 조회
  /*
  @Transactional
  public QrTicketResponseDto issueGuest(String token) {

    // JWT token 권한 파싱
    Claims claims = QrLinkTokenGenerator.getClaimsFromToken(token);

    QrTicketRequestDto dto = QrTicketRequestDto.builder()
        .ticketId((Long) claims.get("ticketId"))
        .eventId((Long) claims.get("eventId"))
        .reservationId((Long) claims.get("reservationId"))
        .attendeeId((Long) claims.get("attendeeId"))
        .build();

    // QR 티켓 조회해 qr code, manualcode 생성해서 반환
    QrTicket qrTicket = qrTicketInitProvider.load(dto, 2);
    qrTicket.setQrCode(codeGenerator.generateRandomToken());
    qrTicket.setManualCode(codeGenerator.generateManualCode());
    return new QrTicketResponseDto();
  }
  */

  // 비회원 QR 티켓 링크 발급 -> 스케쥴러가 실행. 오전9시 실행 batch 도입 예정
  public void generateQrLink() {
    //attendee_type_code = 2인 참석자만 추출 (GUEST)
    // jwt Token 생성해 qr url 생성
    // 각 attendee의 email로 qr티켓 전송
  }

  // QR 티켓 엔티티 생성 - 스케쥴러가 실행
  @Transactional
  public void createQrTicket() {
//    List<QrTicket> qrTickets = qrTicketInitProvider.scheduleCreateQrTicket();
//    qrTicketRepository.saveAll(qrTickets);
  }
}
