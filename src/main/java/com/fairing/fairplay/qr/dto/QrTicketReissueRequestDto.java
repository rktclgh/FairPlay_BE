package com.fairing.fairplay.qr.dto;

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
public class QrTicketReissueRequestDto {

  private String ticketNo; // 재발급 요청한 티켓 번호
}
