package com.fairing.fairplay.review.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDto {

  private Long reviewId; // 리뷰 ID
  private EventDto event; // 리뷰 작성한 행사 정보
  private Integer star; // 리뷰 별점
  private Long reactions; // 리뷰 좋아요
  private String comment; // 리뷰 내용
  private Boolean isPublic; // 리뷰 공개 여부
  private LocalDateTime createdAt; // 리뷰 작성
  private Boolean isUpdated; // 리뷰 수정 여부
}