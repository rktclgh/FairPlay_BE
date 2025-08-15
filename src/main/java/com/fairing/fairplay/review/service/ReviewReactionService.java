package com.fairing.fairplay.review.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.review.dto.ReactionRequestDto;
import com.fairing.fairplay.review.dto.ReactionResponseDto;
import com.fairing.fairplay.review.entity.Review;
import com.fairing.fairplay.review.entity.ReviewReaction;
import com.fairing.fairplay.review.entity.ReviewReactionId;
import com.fairing.fairplay.review.repository.ReviewReactionRepository;
import com.fairing.fairplay.review.repository.ReviewRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewReactionService {

  private final ReviewReactionRepository reviewReactionRepository;
  private final ReviewRepository reviewRepository;
  private final UserRepository userRepository;

  @Transactional
  public ReactionResponseDto toggleReaction(CustomUserDetails customUserDetails,
      ReactionRequestDto dto) {
    if (dto == null || dto.getReviewId() == null) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "리뷰ID는 필수입니다.");
    }
    Long userId = customUserDetails.getUserId();
    // 사용자 조회
    Users user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "사용자가 조회되지 않습니다."));

    // 리뷰 조회
    Review review = reviewRepository.findById(dto.getReviewId()).orElseThrow(
        () -> new CustomException(HttpStatus.NOT_FOUND, "리뷰가 조회되지 않습니다.")
    );

    // 본인이 작성한 리뷰에 좋아요 반응을 추가하려하는 경우
    if (review.getUser().getUserId().equals(user.getUserId())) {
      throw new CustomException(HttpStatus.FORBIDDEN, "본인이 작성한 리뷰에는 좋아요 반응을 할 수 없습니다.");
    }

    // 기존 반응 여부 확인 -> 있으면 review에 대한 좋아요 반응 한 상태이므로 삭제. 없으면 추가
    Optional<ReviewReaction> existingReaction = reviewReactionRepository.findByReviewAndUser(review,
        user);

    if (existingReaction.isPresent()) {
      return deleteReaction(existingReaction.get(), review);
    }
    try {
      return saveReaction(review, user);
    } catch (DataIntegrityViolationException e) {
      long count = reviewReactionRepository.countByReview(review);
      return buildReactionResponse(review, count);
    }
  }

  // 여러 리뷰 ID에 대한 "좋아요(리액션) 개수 한번에 조회
  public Map<Long, Long> findReactionCountsByReviewIds(List<Long> reviewIds) {
    // 조회할 리뷰 ID 없으면 빈 Map 반환
    if (reviewIds.isEmpty()) {
      return Collections.emptyMap();
    }

    // [리뷰ID, 좋아요수]
    List<Object[]> results = reviewReactionRepository.countReactionsByReviewIds(reviewIds);

    return results.stream()
        .collect(Collectors.toMap(
            r -> {
              try {
                return ((Number) r[0]).longValue(); // 리뷰ID
              } catch (ClassCastException e) {
                throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "리뷰 반응 카운트 조회 중 오류가 발생했습니다."); // 타입 변환 실패 시 발생
              }
            },
            r -> {
              try {
                return ((Number) r[1]).longValue(); // 좋아요 갯수
              } catch (ClassCastException e) {
                throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "리뷰 반응 카운트 조회 중 오류가 발생했습니다.");
              }
            }
        ));
  }

  private ReactionResponseDto saveReaction(Review review, Users user) {
    ReviewReaction reviewReaction = buildReviewReaction(review, user);
    reviewReactionRepository.save(reviewReaction);
    long count = reviewReactionRepository.countByReview(review);
    return buildReactionResponse(
        review,
        count
    );
  }

  private ReactionResponseDto deleteReaction(ReviewReaction reviewReaction, Review review) {
    reviewReactionRepository.delete(reviewReaction);
    long count = reviewReactionRepository.countByReview(review);

    return buildReactionResponse(
        review,
        count
    );
  }

  private ReviewReaction buildReviewReaction(Review review, Users user) {
    ReviewReactionId id = new ReviewReactionId(review.getId(), user.getUserId());
    return ReviewReaction.builder()
        .id(id)
        .review(review)
        .user(user)
        .build();
  }

  private ReactionResponseDto buildReactionResponse(Review review, Long count) {
    return ReactionResponseDto.builder()
        .reviewId(review.getId())
        .count(count)
        .build();
  }
}
