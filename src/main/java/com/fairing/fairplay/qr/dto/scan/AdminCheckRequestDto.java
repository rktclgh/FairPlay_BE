package com.fairing.fairplay.qr.dto.scan;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.qr.entity.QrActionCode;
import com.fairing.fairplay.qr.entity.QrCheckStatusCode;
import com.fairing.fairplay.qr.entity.QrTicket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminCheckRequestDto {

  private Attendee attendee;// 회원
  private QrTicket qrTicket; // 티켓
  private QrActionCode qrActionCode; // FORCE_CHECKED_IN, FORCE_CHECKED_OUT
  private QrCheckStatusCode qrCheckStatusCode; // ENTRY, EXIT
}
