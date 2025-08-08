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
public class GuestQrCheckInRequestDto {

  private String qrLinkToken;
  private String qrCode;
}
