package com.fairing.fairplay.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 리뷰 저장 요청
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSaveRequestDto {

  private Long reservationId;
  private Integer star;
  private boolean isPublic;
  private String comment;
}
