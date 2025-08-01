package com.fairing.fairplay.review.repository;

import com.fairing.fairplay.review.entity.ReviewReaction;
import com.fairing.fairplay.review.entity.ReviewReactionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReactionRepository extends JpaRepository<ReviewReaction, ReviewReactionId> {

}
