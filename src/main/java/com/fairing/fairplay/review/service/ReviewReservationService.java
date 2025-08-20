package com.fairing.fairplay.review.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.review.dto.PossibleReviewResponseDto;
import com.fairing.fairplay.review.repository.ReviewRepository;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewReservationService {

  private final ReservationRepository reservationRepository;
  private final ReviewRepository reviewRepository;

  public Reservation checkReservationIdAndUser(Long reservationId, Long userId) {
    return reservationRepository.findByReservationIdAndUser_UserId(reservationId, userId).orElseThrow(() ->
        new CustomException(HttpStatus.FORBIDDEN, "해당 사용자의 예약 정보를 찾을 수 없습니다."));
  }

  public void checkReservationIsCancelled(Reservation reservation) {
    if (reservation.isCanceled()) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "예약이 취소되어 리뷰를 작성하실 수 없습니다.");
    }
  }

  // 마이페이지 리뷰 작성 가능한 행사 조회
  public Page<PossibleReviewResponseDto> getPossibleSaveReview(CustomUserDetails userDetails,
      int page) {
    Pageable pageable = PageRequest.of(page, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
    if (userDetails == null) {
      throw new CustomException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
    }

    if (page < 0) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "page는 0 이상의 정수여야 합니다.");
    }

    // 1. 로그인한 사용자가 리뷰를 작성할 수 있는 모든 행사 목록 페이징
    Page<PossibleReviewResponseDto> reservations = reservationRepository.findPossibleReviewReservationsDto(
        userDetails.getUserId(), pageable);
    log.info("Page<PossibleReviewResponseDto> reservations:{}", reservations.getSize());

    // 2. 페이지로 조회한 예약 DTO 목록에서 예약 ID만 뽑아 리스트 생성
    List<Long> reservationIds = reservations.getContent().stream()
        .map(PossibleReviewResponseDto::getReservationId)
        .collect(Collectors.toList());
    log.info("Page<PossibleReviewResponseDto> reservations:{}", reservationIds.size());

    // 3. 1번에서 조회된 행사들의 reservationId 목록 이용해 작성자가 이미 작성한 리뷰가 있는 reservationId만 Set 형태로 가져옴
    Set<Long> reviewedIds = reservationIds.isEmpty() ? Collections.emptySet()
        : reviewRepository.findReviewedReservationIds(reservationIds);
    log.info("Page<PossibleReviewResponseDto> reservations:{}", reviewedIds.size());

    // 4. 각 행사 DTO에 대해 reservationId가 reviewedIds Set에 있으면 hasReview = true 아니면 hasReview = false
    reservations.getContent()
        .forEach(dto -> dto.setHasReview(reviewedIds.contains(dto.getReservationId())));
    return reservations;

  }
}