package com.fairing.fairplay.review.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PossibleReviewResponseDto {
  private Long reservationId;
  private EventDto event;
  private String ticketContent;
}
