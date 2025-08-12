package com.fairing.fairplay.qr.dto.scan;

import com.fairing.fairplay.attendee.entity.Attendee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckInRequestDto {

  private Attendee attendee;//회원
  private String codeValue;// qr 또는 manual 실제 값
  private String codeType;// QR or MANUAL
  private String qrActionCode; // CHECKED_IN, MANUAL_CHECKED_IN
}