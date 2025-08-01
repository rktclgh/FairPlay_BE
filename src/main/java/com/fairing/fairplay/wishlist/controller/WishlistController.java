package com.fairing.fairplay.wishlist.controller;

import com.fairing.fairplay.wishlist.dto.WishlistResponseDto;
import com.fairing.fairplay.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    // 찜 등록
    @PostMapping
    public ResponseEntity<Void> addWishlist(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long eventId
    ) {
        wishlistService.addWishlist(userId, eventId);
        return ResponseEntity.ok().build();
    }

    // 찜 취소
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> cancelWishlist(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long eventId
    ) {
        wishlistService.cancelWishlist(userId, eventId);
        return ResponseEntity.ok().build();
    }

    // 찜 목록 조회
    @GetMapping
    public ResponseEntity<List<WishlistResponseDto>> getWishlist(
            @AuthenticationPrincipal Long userId
    ) {
        List<WishlistResponseDto> wishlist = wishlistService.getMyWishlist(userId);
        return ResponseEntity.ok(wishlist);
    }
}
