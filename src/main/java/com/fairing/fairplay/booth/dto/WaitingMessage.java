package com.fairing.fairplay.booth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WaitingMessage {
  private Integer waitingCount;
  private String statusMessage;
}
