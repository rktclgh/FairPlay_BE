package com.fairing.fairplay.review.repository;

import com.fairing.fairplay.review.entity.Review;
import com.fairing.fairplay.review.entity.ReviewReaction;
import com.fairing.fairplay.review.entity.ReviewReactionId;
import com.fairing.fairplay.user.entity.Users;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewReactionRepository extends JpaRepository<ReviewReaction, ReviewReactionId> {

  @Query("SELECT r.review.id, COUNT(r) FROM ReviewReaction r WHERE r.review.id IN :reviewIds GROUP BY r.review.id")
  List<Object[]> countReactionsByReviewIds(@Param("reviewIds") List<Long> reviewIds);

  Optional<ReviewReaction> findByReviewAndUser(Review review, Users user);

  long countByReview(Review review);
}
