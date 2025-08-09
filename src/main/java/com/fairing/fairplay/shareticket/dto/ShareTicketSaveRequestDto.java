package com.fairing.fairplay.shareticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareTicketSaveRequestDto {

  private Long reservationId; // 예약ID
  private Integer totalAllowed; // 구매한 티켓 총 개수
}
