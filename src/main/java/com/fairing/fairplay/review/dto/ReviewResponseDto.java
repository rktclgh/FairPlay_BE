package com.fairing.fairplay.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 리뷰 조회 응답
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDto {

  private Long reservationId;
  private EventDto event; // 리뷰 작성한 행사 정보
  private ReviewDto review;
  private boolean owner;
}