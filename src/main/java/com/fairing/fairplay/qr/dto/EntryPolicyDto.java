package com.fairing.fairplay.qr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EntryPolicyDto {

  @Builder.Default
  private boolean checkInAllowed = false;
  @Builder.Default
  private boolean checkOutAllowed = false;
  @Builder.Default
  private boolean reentryAllowed = false;
}
