package com.fairing.fairplay.review.entity;

import com.fairing.fairplay.user.entity.Users;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "review_reaction")
public class ReviewReaction {

  @EmbeddedId
  private ReviewReactionId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("reviewId")
  @JoinColumn(name = "review_id")
  private Review review;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  @JoinColumn(name = "user_id")
  private Users user;
}
