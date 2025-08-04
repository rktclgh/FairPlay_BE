package com.fairing.fairplay.shareticket.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareTicketSaveRequestDto {

  private Long reservationId;
  private Integer totalAllowed;
  private LocalDateTime expiredAt;
}
