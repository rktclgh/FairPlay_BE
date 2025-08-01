package com.fairing.fairplay.review.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.review.dto.EventDto;
import com.fairing.fairplay.review.dto.ReviewDeleteResponseDto;
import com.fairing.fairplay.review.dto.ReviewDto;
import com.fairing.fairplay.review.dto.ReviewResponseDto;
import com.fairing.fairplay.review.dto.ReviewSaveRequestDto;
import com.fairing.fairplay.review.entity.Review;
import com.fairing.fairplay.review.repository.ReviewRepository;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final UserRepository userRepository;
  private final EventRepository eventRepository;
  private final ReviewReactionService reviewReactionService;

  // 본인이 예매한 행사에 대해서만 리뷰 작성 가능
  @Transactional
  public void save(ReviewSaveRequestDto dto) {
    // user 임시 설정
    Users user = userRepository.getReferenceById(1L);

    // reservation 검증 추가 예정
    Review review = Review.builder()
        .user(user)
        .reservationId(dto.getReservationId())
        .comment(dto.getComment())
        .isPublic(dto.getIsPublic())
        .star(dto.getStar())
        .build();

    reviewRepository.save(review);
  }

  // 행사 상세 페이지 - 특정 행사 리뷰 조회
  public Page<ReviewResponseDto> getReviewForEvent(Long eventId, Pageable pageable) {

//    // 1. 리뷰 페이징 조회
//    Page<Review> reviewPage = reviewRepository.findByEventId(eventId, pageable);
//
//    // 2. 리뷰 ID 추출
//    List<Long> reviewIds = reviewPage.stream()
//        .map(Review::getId)
//        .toList();
//
//    // 3. 리뷰별 좋아요 갯수 한번에 조회
//    Map<Long, Long> reactionCountMap = reviewReactionService.findReactionCountsByReviewIds(
//        reviewIds);
//
//    // 4. DTO 변환
//    return reviewPage.map(review -> {
//      long reactionCount = reactionCountMap.getOrDefault(review.getId(), 0L);
//      return toReviewResponseDto(review, reactionCount);
//    });
    return null;
  }

  // 마이페이지 - 본인의 모든 리뷰 조회
//  public Page<ReviewResponseDto> getReviewForUser() {
//
//  }

  // 리뷰 삭제
  @Transactional
  public ReviewDeleteResponseDto deleteReview(Long userId, Long reviewId) {
    // 1.  사용자 존재 여부 확인
    Users user = userRepository.findById(userId)
        .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "사용자를 조회할 수 없습니다."));

    // 2. 리뷰 작성자와 요청 사용자 일치 여부 확인
    Review review = reviewRepository.findByIdAndUser(reviewId, user)
        .orElseThrow(() -> new IllegalArgumentException("리뷰를 작성한 사용자와 로그인한 사용자가 일치하지 않습니다."));

    // 3. 리뷰 삭제
    reviewRepository.delete(review);

    return ReviewDeleteResponseDto.builder()
        .reviewId(reviewId)
        .message("리뷰가 정상적으로 삭제되었습니다.")
        .build();
  }

  private ReviewResponseDto toReviewResponseDto(Review review, Long reactionCount) {
    // Event event = review.getReservation(); // 엔티티 기준
    // event 조회
    Event event = eventRepository.findById(1L)
        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
            "이벤트를 찾을 수 없습니다. id=" + review.getReservationId()));

    // 조회한 리뷰 관련 행사 정보
    EventDto eventDto = EventDto.builder()
        .title(event.getTitleKr())
        .buildingName(event.getEventDetail().getLocation().getBuildingName())
        .address(event.getEventDetail().getLocation().getAddress())
        //event_schedule 테이블 조회 예정
        .viewingScheduleInfo(EventDto.ViewingScheduleInfo.builder()
            .date("2025-08-01")
            .dayOfWeek("금")
            .startTime("14:00")
            .build())
        //event_detail 테이블 조회
        .eventScheduleInfo(EventDto.EventScheduleInfo.builder()
            .startDate(getDate(event.getEventDetail().getStartDate()))
            .endDate(getDate(event.getEventDetail().getEndDate()))
            .build())
        .build();

    ReviewDto reviewDto = ReviewDto.builder()
        .reviewId(review.getId())
        .star(review.getStar())
        .reactions(reactionCount)
        .comment(review.getComment())
        .isPublic(review.getIsPublic())
        .createdAt(review.getCreatedAt())
        .isUpdated(review.getUpdatedAt() != null)
        .build();

    return new ReviewResponseDto(
        eventDto,
        reviewDto
    );
  }

  private String getDate(LocalDate date) {
    return date.format(DateTimeFormatter.ofPattern("yyyy. MM. dd"));
  }

  private String getDayOfWeek(int weekday) {
    int dayOfWeekValue = (weekday == 0) ? 7 : weekday; // 0(일)은 7로 매핑
    return DayOfWeek.of(dayOfWeekValue).getDisplayName(TextStyle.FULL, Locale.KOREAN);
  }

  private String getTime(LocalTime time) {
    return time.format(DateTimeFormatter.ofPattern("HH:mm"));
  }
}
