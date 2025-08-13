package com.fairing.fairplay.review.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
public class PossibleReviewResponseDto {

  private Long reservationId;
  private EventDto event;
  private String ticketContent;
  private boolean hasReview;
}
