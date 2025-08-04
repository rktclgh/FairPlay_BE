package com.fairing.fairplay.qr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QrTicketRequestDto {

  private Long attendeeId;
  private Long eventId;
  private Long ticketId;
  private Long reservationId;
}
