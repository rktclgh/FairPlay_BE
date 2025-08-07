package com.fairing.fairplay.qr.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// QR 티켓 강제 재발급 응답
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QrTicketReissueResponseDto {

  private String ticketNo; // 발급된 티켓 번호
  private String email; // 이메일
}
