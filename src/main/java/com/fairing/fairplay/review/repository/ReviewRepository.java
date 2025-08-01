package com.fairing.fairplay.review.repository;

import com.fairing.fairplay.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

}
