package com.fairing.fairplay.review.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 리뷰 저장 응답
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSaveResponseDto {

  private Long reviewId; // 리뷰 ID
  private String nickname; // 리뷰 닉네임
  private Integer star; // 리뷰 별점
  private String comment; // 리뷰 내용
  private boolean visible; // 리뷰 공개 여부
  private LocalDateTime createdAt; // 리뷰 작성
}
