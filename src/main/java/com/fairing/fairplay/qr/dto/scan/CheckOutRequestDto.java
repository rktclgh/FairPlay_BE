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
public class CheckOutRequestDto {
  private Attendee attendee;//회원
  private String codeValue;// qr 또는 manual 실제 값
  private String codeType;// QR or MANUAL
  private boolean requireUserMatch;// 회원 체크아웃할때 참석자=로그인정보 일치하는지 여부 조회
}
