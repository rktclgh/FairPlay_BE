package com.fairing.fairplay.shareticket.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShareTicketSaveRequestDto {

  private Long reservationId;
  private int totalAllowed;
  private LocalDateTime expiredAt;
}
