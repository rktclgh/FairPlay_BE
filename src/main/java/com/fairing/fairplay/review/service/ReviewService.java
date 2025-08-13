package com.fairing.fairplay.review.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.review.dto.EventDto;
import com.fairing.fairplay.review.dto.ReviewDeleteResponseDto;
import com.fairing.fairplay.review.dto.ReviewDto;
import com.fairing.fairplay.review.dto.ReviewForEventResponseDto;
import com.fairing.fairplay.review.dto.ReviewResponseDto;
import com.fairing.fairplay.review.dto.ReviewSaveRequestDto;
import com.fairing.fairplay.review.dto.ReviewSaveResponseDto;
import com.fairing.fairplay.review.dto.ReviewUpdateRequestDto;
import com.fairing.fairplay.review.dto.ReviewUpdateResponseDto;
import com.fairing.fairplay.review.dto.ReviewWithOwnerDto;
import com.fairing.fairplay.review.entity.Review;
import com.fairing.fairplay.review.repository.ReviewRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final UserRepository userRepository;
  private final ReviewReactionService reviewReactionService;
  private final ReviewReservationService reviewReservationService;

  // 본인이 예매한 행사에 대해서만 리뷰 작성 가능
  @Transactional
  public ReviewSaveResponseDto save(ReviewSaveRequestDto dto) {
    // user 임시 설정
    Users user = findUserOrThrow(1L);

    // 1. user의 사용자 권한이 일반 사용자인지 검증
//    if (!user.getRoleCode().getCode().equals("COMMON")) {
//      throw new CustomException(HttpStatus.FORBIDDEN,
//          "리뷰를 작성할 사용자 타입이 아닙니다. 현재 사용자 타입: " + user.getRoleCode().getCode());
//    }

    // 2. 리뷰 작성자와 예약자가 일치하는지 조회
    Reservation reservation = reviewReservationService.checkReservationIdAndUser(
        dto.getReservationId(), user);

    // 3. 예약이 취소되었는지 검증
    reviewReservationService.checkReservationIsCancelled(reservation);

    // 4. 리뷰 작성 가능 기간 제한
    LocalDate endDate = reservation.getSchedule().getDate();
    validateReviewPeriod(endDate);

    // 5. 이미 작성한 리뷰가 있는지 조회
    if (reviewRepository.existsByReservationAndUser(reservation, user)) {
      throw new CustomException(HttpStatus.CONFLICT, "이미 리뷰를 작성한 행사입니다.");
    }

    // 6. 리뷰 별점 유효성 검증
    validateStar(dto.getStar());

    Review review = Review.builder()
        .user(user)
        .reservation(reservation)
        .comment(dto.getComment())
        .visible(dto.isVisible())
        .star(dto.getStar())
        .build();

    Review saveReview = reviewRepository.save(review);
    return buildReviewSaveResponse(saveReview);
  }

  // 행사 상세 페이지 - 특정 행사 리뷰 조회. CustomUserDetails 추가 예정
  public ReviewForEventResponseDto getReviewForEvent(CustomUserDetails userDetails, Long eventId,
      Pageable pageable) {
    log.info("getReviewForEvent:{}", eventId);
    Long loginUserId;
    if (userDetails != null) {
      loginUserId = userDetails.getUserId();
    } else {
      loginUserId = null;
    }

    // 특정 이벤트 리뷰 조회
    Page<Review> reviewPage = reviewRepository.findByEventId(eventId, pageable);

    // 리뷰 ID 추출
    List<Long> reviewIds = reviewPage.stream()
        .map(Review::getId)
        .toList();
    log.info("reviewIds:{}", reviewIds.getFirst());

    // 이벤트의 리뷰들에 대한 카운트
    Map<Long, Long> reactionCountMap =
        reviewIds.isEmpty() ? Collections.emptyMap()
            : reviewReactionService.findReactionCountsByReviewIds(reviewIds);

    Page<ReviewWithOwnerDto> reviewWithOwnerDtos = reviewPage.map(review -> {
      long reactionCount = reactionCountMap.getOrDefault(review.getId(), 0L);
      log.info("reactionCount:{}", reactionCount);
      boolean isMine = (loginUserId != null) && loginUserId.equals(review.getUser().getUserId());
      log.info("isMine:{}", isMine);
      ReviewDto reviewDto = buildReview(review, reactionCount);
      return ReviewWithOwnerDto.builder()
          .review(reviewDto)
          .owner(isMine)
          .build();
    });

    return ReviewForEventResponseDto.builder()
        .eventId(eventId)
        .reviews(reviewWithOwnerDtos).build();
  }

  // 마이페이지 - 본인의 모든 리뷰 조회
  public Page<ReviewResponseDto> getReviewForUser(Long userId, Pageable pageable) {
    // 1.  사용자 존재 여부 확인
    Users user = findUserOrThrow(userId);

    // 2. 리뷰 페이징 조회
    Page<Review> reviewPage = reviewRepository.findByUser(user, pageable);

    // 3. 리뷰 ID 추출
    List<Long> reviewIds = reviewPage.stream()
        .map(Review::getId)
        .toList();

    // 4. 리뷰별 좋아요 갯수 한번에 조회
    Map<Long, Long> reactionCountMap = reviewReactionService.findReactionCountsByReviewIds(
        reviewIds);

    // 5. DTO 변환
    return reviewPage.map(review -> {
      long reactionCount = reactionCountMap.getOrDefault(review.getId(), 0L);
      return buildReviewResponse(review, reactionCount, true);
    });
  }

  // 리뷰 수정 (리액션 제외)
  @Transactional
  public ReviewUpdateResponseDto updateReview(Long userId, Long reviewId,
      ReviewUpdateRequestDto dto) {
    // 1.  사용자 존재 여부 확인
    Users user = findUserOrThrow(userId);

    // 2. 사용자 타입 확인
    if (!user.getRoleCode().getCode().equals("COMMON")) {
      throw new CustomException(HttpStatus.FORBIDDEN,
          "해당 리뷰를 수정할 수 있는 사용자 타입이 아닙니다. 현재 사용자 타입: " + user.getRoleCode().getCode());
    }

    // 3.  리뷰 조회
    Review review = reviewRepository.findByIdAndUser(reviewId, user)
        .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "리뷰를 조회할 수 없습니다."));

    // 4. 리뷰 수정 가능 기한 검증
    LocalDate endDate = review.getReservation().getSchedule().getDate();
    validateReviewPeriod(endDate);

    // 5. 별점 검증
    validateStar(dto.getStar());

    // 6.  리뷰 수정 (createdAt, reaction 수정 불가)
    review.setStar(dto.getStar());
    review.setVisible(dto.getVisible());
    review.setComment(dto.getComment());
    review.setUpdatedAt(LocalDateTime.now());

    return ReviewUpdateResponseDto.builder()
        .reviewId(reviewId)
        .comment(dto.getComment())
        .visible(dto.getVisible())
        .star(dto.getStar())
        .build();
  }

  // 리뷰 삭제
  @Transactional
  public ReviewDeleteResponseDto deleteReview(Long userId, Long reviewId) {
    // 1.  사용자 존재 여부 확인
    Users user = findUserOrThrow(userId);
    // 2. 사용자 권한 확인
    if (!user.getRoleCode().getCode().equals("COMMON")) {
      throw new CustomException(HttpStatus.FORBIDDEN,
          "해당 리뷰를 수정할 수 있는 사용자 타입이 아닙니다. 현재 사용자 타입: " + user.getRoleCode().getCode());
    }

    // 3. 리뷰 작성자와 요청 사용자 일치 여부 확인
    Review review = reviewRepository.findByIdAndUser(reviewId, user)
        .orElseThrow(
            () -> new CustomException(HttpStatus.FORBIDDEN, "리뷰 작성자와 일치하지 않으므로 삭제하실 수 없습니다."));

    // 4. 리뷰와 연결된 예약이 존재하는지 확인
    Reservation reservation = reviewReservationService.checkReservationIdAndUser(
        review.getReservation().getReservationId(), user);

    // 5. 삭제 가능 기한 검증
    LocalDate endDate = reservation.getSchedule().getDate();
    validateDeletePeriod(endDate);

    // 6. 리뷰 삭제
    reviewRepository.delete(review);

    return ReviewDeleteResponseDto.builder()
        .reviewId(reviewId)
        .message("리뷰가 정상적으로 삭제되었습니다.")
        .build();
  }

  private ReviewSaveResponseDto buildReviewSaveResponse(Review review) {
    return ReviewSaveResponseDto.builder()
        .reviewId(review.getId())
        .comment(review.getComment())
        .star(review.getStar())
        .visible(review.isVisible())
        .createdAt(review.getCreatedAt())
        .build();
  }

  private ReviewResponseDto buildReviewResponse(Review review, Long reactionCount, boolean owner) {
    EventDto eventDto = buildEvent(review.getReservation());
    ReviewDto reviewDto = buildReview(review, reactionCount);
    return new ReviewResponseDto(eventDto, reviewDto, owner);
  }

  private EventDto buildEvent(Reservation reservation) {
    return reviewRepository.findEventDtoByReservationId(reservation.getReservationId())
        .orElse(null);
  }

  private ReviewDto buildReview(Review review, Long reactionCount) {
    return ReviewDto.builder()
        .reviewId(review.getId())
        .nickname(review.getUser().getNickname())
        .star(review.getStar())
        .reactions(reactionCount)
        .comment(review.getComment())
        .visible(review.isVisible())
        .createdAt(review.getCreatedAt())
        .build();
  }

  // 사용자 조회
  private Users findUserOrThrow(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "사용자를 조회할 수 없습니다."));
  }

  // 별점 검증
  private void validateStar(Integer star) {
    if (star == null || star < 0 || star > 5) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "별점은 0~5 사이어야 합니다.");
    }
  }

  // 리뷰 작성 가능 기간 검증
  private void validateReviewPeriod(LocalDate endDate) {
    LocalDate now = LocalDate.now();

    if (now.isBefore(endDate)) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "행사가 종료된 후에만 리뷰를 작성하실 수 있습니다.");
    }
    if (endDate.plusDays(7).isBefore(LocalDate.now())) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "리뷰 작성 기간이 만료되었습니다.");
    }
  }

  // 리뷰 삭제 가능 기간 검증
  private void validateDeletePeriod(LocalDate endDate) {
    LocalDate now = LocalDate.now();
    if (endDate.plusDays(30).isBefore(now)) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "리뷰 삭제 가능 기간이 만료되었습니다.");
    }
  }
}
