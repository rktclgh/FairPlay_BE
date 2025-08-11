package com.fairing.fairplay.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 리뷰 수정 응답
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewUpdateResponseDto {

  private Long reviewId; // 리뷰ID
  private Integer star; // 별점
  private String comment; // 내용
  private boolean isPublic; // 공개 여부
}
