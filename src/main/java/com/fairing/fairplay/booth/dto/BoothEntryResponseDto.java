package com.fairing.fairplay.booth.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoothEntryResponseDto {
  private String name;
  private String message;
  private LocalDateTime checkInTime;
}
