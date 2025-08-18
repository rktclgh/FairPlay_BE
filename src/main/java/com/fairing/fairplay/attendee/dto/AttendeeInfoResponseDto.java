package com.fairing.fairplay.attendee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttendeeInfoResponseDto {

  private Long reservationId;
  private Long attendeeId;
  private String name;
  private String email;
  private String phone;
  private Boolean agreeToTerms;
}
