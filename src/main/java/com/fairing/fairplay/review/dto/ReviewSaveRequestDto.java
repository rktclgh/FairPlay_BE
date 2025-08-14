package com.fairing.fairplay.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 리뷰 저장 요청
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSaveRequestDto {

  private Long reservationId;
  private Integer star;
  private boolean visible;
  private String comment;
}
