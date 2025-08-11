package com.fairing.fairplay.qr.dto.scan;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckOutResponseDto {
  private String message;
  private LocalDateTime checkInTime;
}
