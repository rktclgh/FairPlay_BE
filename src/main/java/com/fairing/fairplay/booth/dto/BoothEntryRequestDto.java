package com.fairing.fairplay.booth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoothEntryRequestDto {

  private Long boothReservationId; // 체험 예약 ID
  private Long boothId; // 부스 ID
  private Long eventId; // 행사 ID
  private String qrCode; // QR 코드
  private String manualCode; // 수동 코드
}
