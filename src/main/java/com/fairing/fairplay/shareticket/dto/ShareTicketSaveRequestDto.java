package com.fairing.fairplay.shareticket.dto;

import com.fairing.fairplay.reservation.entity.Reservation;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShareTicketSaveRequestDto {

  private Reservation reservation;
  private int totalAllowed;
  private LocalDateTime expiredAt;
}
