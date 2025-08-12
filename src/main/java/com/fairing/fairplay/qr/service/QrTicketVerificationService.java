package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrTicketRepository;
import com.fairing.fairplay.qr.util.CodeValidator;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QrTicketVerificationService {

  private final QrTicketRepository qrTicketRepository;
  private final CodeValidator codeValidator;
  private final UserRepository userRepository;

  /**
   * 참석자가 회원이 맞는지 검증
   */
  public void validateUser(Users user) {
    if (!userRepository.existsById(user.getUserId())) {
      throw new CustomException(HttpStatus.FORBIDDEN, "티켓 예약자가 회원이 아닙니다. 관리자에게 문의하세요.");
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
