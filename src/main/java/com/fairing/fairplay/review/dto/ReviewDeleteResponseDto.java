package com.fairing.fairplay.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 삭제 응답
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDeleteResponseDto {
  private Long reviewId;
  private String message;
}
