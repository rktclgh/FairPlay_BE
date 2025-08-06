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
public class AttendeeSaveRequestDto {

  private String name;
  private String email;
  private String phone;
  private LocalDate birth;
}
