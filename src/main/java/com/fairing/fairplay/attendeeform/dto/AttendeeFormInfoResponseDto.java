package com.fairing.fairplay.attendeeform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendeeFormInfoResponseDto {

  private Long formId;
  private Long eventId;
  private String eventName;
}
