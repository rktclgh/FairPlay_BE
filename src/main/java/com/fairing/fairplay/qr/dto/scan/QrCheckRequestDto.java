package com.fairing.fairplay.qr.dto.scan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QrCheckRequestDto {

  private String qrLinkToken;
  private String qrCode;
}