package com.fairing.fairplay.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ReviewReactionId implements Serializable {

  @Column(name = "review_id", nullable = false, updatable = false)
  private Long reviewId;

  @Column(name = "user_id", nullable = false, updatable = false)
  private Long userId;
}
