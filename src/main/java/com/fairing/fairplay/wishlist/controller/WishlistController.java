package com.fairing.fairplay.wishlist.controller;

import com.fairing.fairplay.wishlist.dto.WishlistResponseDto;
import com.fairing.fairplay.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.fairing.fairplay.core.security.CustomUserDetails;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    // 찜 등록
    @PostMapping
    public ResponseEntity<String> addWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long eventId
    ) {
        Long userId = userDetails.getUserId();
        wishlistService.addWishlist(userId, eventId);
        return ResponseEntity.ok("찜 등록이 완료되었습니다.");
    }

    // 찜 취소
    @DeleteMapping("/{eventId}")
    public ResponseEntity<String> cancelWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId
    ) {
        Long userId = userDetails.getUserId();
        wishlistService.cancelWishlist(userId, eventId);
        return ResponseEntity.ok("삭제되었습니다.");
    }

    // 찜 목록 조회
    @GetMapping
    public ResponseEntity<List<WishlistResponseDto>> getWishlist(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        List<WishlistResponseDto> wishlist = wishlistService.getMyWishlist(userId);
        return ResponseEntity.ok(wishlist);
    }
}
