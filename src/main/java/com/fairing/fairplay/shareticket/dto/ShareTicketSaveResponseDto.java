package com.fairing.fairplay.shareticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareTicketSaveResponseDto {

  private Long reservationId;
  private String token;
}
