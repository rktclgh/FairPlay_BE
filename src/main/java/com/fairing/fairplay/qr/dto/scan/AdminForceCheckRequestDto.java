package com.fairing.fairplay.qr.dto.scan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminForceCheckRequestDto {

  private String ticketNo; // 티켓 번호
  private String qrCheckStatusCode; // ENTRY, EXIT
}
