package com.fairing.fairplay.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewWithOwnerDto {
  private ReviewDto review;
  private boolean owner;
  private boolean liked;
}
