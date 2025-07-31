package com.fairing.fairplay.qr.dto;

import com.fairing.fairplay.attendee.entity.Attendee;
import com.fairing.fairplay.ticket.entity.EventTicket;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QrTicketInitInfo {
  private Attendee attendee;
  private EventTicket eventTicket;
  private boolean reentryAllowed;
  private LocalDateTime expiredAt;

}
