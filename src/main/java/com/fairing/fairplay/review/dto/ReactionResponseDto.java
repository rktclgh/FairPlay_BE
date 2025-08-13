package com.fairing.fairplay.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionResponseDto {

  private Long reviewId; // 리뷰
  private Long count; // 좋아요 총 갯수
}
