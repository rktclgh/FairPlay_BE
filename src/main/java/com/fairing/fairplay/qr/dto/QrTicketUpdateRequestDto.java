package com.fairing.fairplay.qr.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QrTicketUpdateRequestDto {
  private String qrUrlToken;
}
