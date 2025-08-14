package com.fairing.fairplay.review.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 리뷰 정보
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDto {

  private Long reviewId; // 리뷰 ID
  private String nickname; // 작성자 이름
  private Integer star; // 리뷰 별점
  private Long reactions; // 리뷰 좋아요
  private String comment; // 리뷰 내용
  private boolean visible; // 리뷰 공개 여부
  private LocalDateTime createdAt; // 리뷰 작성
}
