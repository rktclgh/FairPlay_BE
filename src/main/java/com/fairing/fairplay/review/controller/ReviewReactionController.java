package com.fairing.fairplay.review.controller;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.review.dto.ReactionRequestDto;
import com.fairing.fairplay.review.dto.ReactionResponseDto;
import com.fairing.fairplay.review.service.ReviewReactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review-reactions")
@RequiredArgsConstructor
public class ReviewReactionController {

  private final ReviewReactionService reviewReactionService;

  // 리액션 추가
  @PostMapping
  public ResponseEntity<ReactionResponseDto> toggleReaction(@AuthenticationPrincipal
  CustomUserDetails customUserDetails, @RequestBody ReactionRequestDto dto) {
    return ResponseEntity.ok(reviewReactionService.toggleReaction(customUserDetails, dto));
  }
}
