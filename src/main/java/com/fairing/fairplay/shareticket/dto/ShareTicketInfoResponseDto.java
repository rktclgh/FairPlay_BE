package com.fairing.fairplay.shareticket.dto;

import com.fairing.fairplay.shareticket.entity.ShareTicket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareTicketInfoResponseDto {
  private Long formId;
  private Long eventId;
  private String eventName;
}
