package com.fairing.fairplay.attendee.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttendeeUpdateRequestDto {

  private Long reservationId;
  private String name;
  private String email;
  private LocalDate birth;
  private String phone;
}
