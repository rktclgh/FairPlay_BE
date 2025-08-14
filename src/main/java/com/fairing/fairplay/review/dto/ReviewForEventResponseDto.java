package com.fairing.fairplay.review.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewForEventResponseDto {
  private Long eventId;
  private Page<ReviewWithOwnerDto> reviews;
}
