package com.fairing.fairplay.review.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.reservation.entity.Reservation;
import com.fairing.fairplay.reservation.repository.ReservationRepository;
import com.fairing.fairplay.review.dto.PossibleReviewResponseDto;
import com.fairing.fairplay.review.repository.ReviewRepository;
import com.fairing.fairplay.user.entity.Users;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewReservationService {

  private final ReservationRepository reservationRepository;
  private final ReviewRepository reviewRepository;

  public Reservation checkReservationIdAndUser(Long reservationId, Users user) {
    return reservationRepository.findByReservationIdAndUser(reservationId, user).orElseThrow(() ->
        new CustomException(HttpStatus.FORBIDDEN, "해당 사용자의 예약 정보를 찾을 수 없습니다."));
  }

  public void checkReservationIsCancelled(Reservation reservation) {
    if (reservation.isCanceled()) {
      throw new CustomException(HttpStatus.BAD_REQUEST, "예약이 취소되어 리뷰를 작성하실 수 없습니다.");
    }
  }

  // 마이페이지 리뷰 작성 가능한 행사 조회
  public Page<PossibleReviewResponseDto> getPossibleSaveReview(CustomUserDetails userDetails, int page) {
    Pageable pageable = PageRequest.of(page, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<PossibleReviewResponseDto> reservations = reservationRepository.findPossibleReviewReservationsDto(
        userDetails.getUserId(), pageable);

    List<Long> reservationIds = reservations.getContent().stream()
        .map(PossibleReviewResponseDto::getReservationId)
        .collect(Collectors.toList());

    Map<Long, Boolean> reviewMap = reviewRepository.findAllByReservation_ReservationIdIn(reservationIds)
        .stream()
        .collect(Collectors.toMap(r -> r.getReservation().getReservationId(), r -> true));


    reservations.getContent().forEach(dto -> dto.setHasReview(reviewMap.getOrDefault(dto.getReservationId(), false)));

    return reservations;

  }
}