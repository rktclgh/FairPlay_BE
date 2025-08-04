package com.fairing.fairplay.review.controller;

import com.fairing.fairplay.review.dto.ReviewDeleteResponseDto;
import com.fairing.fairplay.review.dto.ReviewResponseDto;
import com.fairing.fairplay.review.dto.ReviewSaveRequestDto;
import com.fairing.fairplay.review.dto.ReviewUpdateRequestDto;
import com.fairing.fairplay.review.dto.ReviewUpdateResponseDto;
import com.fairing.fairplay.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

  private final ReviewService reviewService;

  // 리뷰 저장
  @PostMapping
  public ResponseEntity<Void> saveReview(@RequestBody ReviewSaveRequestDto dto) {
    reviewService.save(dto);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  // 행사 상세 페이지 리뷰 조회
  // GET /events/{eventId}/reviews?page=0&size=10&sort=createdDate,desc
  @GetMapping("/{eventId}")
  public ResponseEntity<Page<ReviewResponseDto>> getReviewForEvent(@PathVariable Long eventId,
      @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(reviewService.getReviewForEvent(eventId, pageable));
  }

  // 마이페이지 리뷰 조회
  @GetMapping
  public ResponseEntity<Page<ReviewResponseDto>> getReviewForUser(
      @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(reviewService.getReviewForUser(1L, pageable));
  }

  // 리뷰 수정 요청 ( 비공개여부, 리뷰 내용 등)
  @PatchMapping("/{reviewId}")
  public ResponseEntity<ReviewUpdateResponseDto> updateReview(@PathVariable Long reviewId,
      @RequestBody ReviewUpdateRequestDto dto) {
    return ResponseEntity.status(HttpStatus.OK).body(reviewService.updateReview(1L, reviewId, dto));
  }

  // 리뷰 삭제
  @DeleteMapping("/{reviewId}")
  public ResponseEntity<ReviewDeleteResponseDto> deleteReview(@PathVariable Long reviewId) {
    return ResponseEntity.status(HttpStatus.OK).body(reviewService.deleteReview(1L, reviewId));
  }
}
