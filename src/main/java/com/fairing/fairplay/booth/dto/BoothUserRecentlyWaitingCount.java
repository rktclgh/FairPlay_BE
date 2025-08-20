package com.fairing.fairplay.booth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BoothUserRecentlyWaitingCount {

  private Long eventId;
  private String eventName;
  private Integer waitingCount;
}
