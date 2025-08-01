package com.fairing.fairplay.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSaveRequestDto {
  private Long reservationId;
  private Integer star;
  private Boolean isPublic;
  private String comment;
}
