package com.fairing.fairplay.qr.dto.scan;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckInResponseDto {
  private String message;
  private LocalDateTime checkInTime;
}
