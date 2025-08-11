package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import com.fairing.fairplay.qr.util.CodeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QrTicketVerificationService {
  private final QrTicketRepository qrTicketRepository;
  private final CodeValidator codeValidator;
  /**
   * 로그인 사용자와 참석자 일치 여부 확인
   */
  public void validateUserMatch(Attendee attendee, CustomUserDetails userDetails) {
    if (!attendee.getReservation().getUser().getUserId().equals(userDetails.getUserId())) {
      throw new CustomException(HttpStatus.FORBIDDEN, "참석자와 로그인한 사용자가 일치하지 않습니다.");
    }
  }

  /**
   * QR 티켓 조회
   */
  public QrTicket findQrTicket(Attendee attendee) {
    return qrTicketRepository.findByAttendee(attendee)
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "예약된 QR 티켓을 찾지 못했습니다."));
  }

  /**
   * 코드 검증
   */
  public void verifyQrCode(QrTicket qrTicket, String qrCode) {
    if (!qrTicket.getActive()) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "만료된 QR 티켓입니다.");
    }

    if (!qrTicket.getQrCode().equals(qrCode)) {
      throw new CustomException(HttpStatus.UNAUTHORIZED, "참석자 정보와 일치하지 않습니다.");
    }
  }

  /**
   * 수동 코드 검증
   */
  public void verifyManualCode(QrTicket qrTicket, String manualCode) {
    codeValidator.validateManualCode(manualCode);

    if (!qrTicket.getManualCode().equals(manualCode)) {
      throw new CustomException(HttpStatus.UNAUTHORIZED, "참석자 정보와 일치하지 않습니다.");
    }
  }
}
