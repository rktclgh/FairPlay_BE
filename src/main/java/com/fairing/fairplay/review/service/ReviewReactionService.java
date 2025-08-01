package com.fairing.fairplay.review.service;

import com.fairing.fairplay.review.repository.ReviewReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReviewReactionService {

  private final ReviewReactionRepository reviewReactionRepository;
}
