package com.fairing.fairplay.review.service;

import com.fairing.fairplay.review.repository.ReviewReactionRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewReactionService {

  private final ReviewReactionRepository reviewReactionRepository;

  // 리뷰별 좋아요 카운트
  public Map<Long, Long> findReactionCountsByReviewIds(List<Long> reviewIds) {
    if (reviewIds.isEmpty()) {
      return Collections.emptyMap();
    }

    List<Object[]> results = reviewReactionRepository.countReactionsByReviewIds(reviewIds);

    return results.stream()
        .collect(Collectors.toMap(
            r -> ((Number) r[0]).longValue(), // review.id
            r -> ((Number) r[1]).longValue()  // COUNT(r)
        ));
  }
}
