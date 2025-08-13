package com.fairing.fairplay.review.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.review.dto.PossibleReviewResponseDto;
import com.fairing.fairplay.review.dto.ReviewDeleteResponseDto;
import com.fairing.fairplay.review.dto.ReviewForEventResponseDto;
import com.fairing.fairplay.review.dto.ReviewResponseDto;
import com.fairing.fairplay.review.dto.ReviewSaveRequestDto;
import com.fairing.fairplay.review.dto.ReviewSaveResponseDto;
import com.fairing.fairplay.review.dto.ReviewUpdateRequestDto;
import com.fairing.fairplay.review.dto.ReviewUpdateResponseDto;
import com.fairing.fairplay.review.service.ReviewReservationService;
import com.fairing.fairplay.review.service.ReviewService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

  private final ReviewService reviewService;
  private final ReviewReservationService reviewReservationService;

  // 리뷰 저장
  @PostMapping
  public ResponseEntity<ReviewSaveResponseDto> saveReview(@RequestBody ReviewSaveRequestDto dto) {
    return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.save(dto));
  }

  // 마이페이지 리뷰 작성한 행사 목록 조회
  @GetMapping("/mypage")
  public ResponseEntity<Page<PossibleReviewResponseDto>> getPossibleSaveReview(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @RequestParam(defaultValue = "0") int page) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(reviewReservationService.getPossibleSaveReview(userDetails, page));
  }

  // 행사 상세 페이지 리뷰 조회
  // GET /events/{eventId}/reviews?page=0&size=10&sort=createdDate,desc
  @GetMapping("/{eventId}")
  public ResponseEntity<ReviewForEventResponseDto> getReviewForEvent(@PathVariable Long eventId,
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(reviewService.getReviewForEvent(userDetails, eventId, pageable));
  }

  // 마이페이지 리뷰 조회
  @GetMapping
  public ResponseEntity<Page<ReviewResponseDto>> getReviewForUser(@AuthenticationPrincipal CustomUserDetails userDetails, int page) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(reviewService.getReviewForUser(userDetails, page));
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
