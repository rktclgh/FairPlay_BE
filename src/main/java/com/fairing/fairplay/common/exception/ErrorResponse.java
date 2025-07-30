package com.fairing.fairplay.common.exception;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class ErrorResponse {

  private int code;
  private String error;
  private String message;
  private LocalDateTime timestamp;

}
