package com.fairing.fairplay.businesscard.controller;

import com.fairing.fairplay.businesscard.dto.BusinessCardRequestDto;
import com.fairing.fairplay.businesscard.dto.BusinessCardResponseDto;
import com.fairing.fairplay.businesscard.dto.CollectedCardResponseDto;
import com.fairing.fairplay.businesscard.service.BusinessCardService;
import com.fairing.fairplay.businesscard.service.BusinessCardQRService;
import com.fairing.fairplay.core.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/business-card")
@RequiredArgsConstructor
public class BusinessCardController {

    private final BusinessCardService businessCardService;
    private final BusinessCardQRService qrService;

    /**
     * 내 전자명함 조회
     */
    @GetMapping("/my")
    public ResponseEntity<BusinessCardResponseDto> getMyBusinessCard(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BusinessCardResponseDto card = businessCardService.getMyBusinessCard(userDetails.getUserId());
        return ResponseEntity.ok(card);
    }

    /**
     * 전자명함 저장/수정
     */
    @PostMapping
    public ResponseEntity<BusinessCardResponseDto> saveBusinessCard(
            @RequestBody BusinessCardRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        BusinessCardResponseDto savedCard = businessCardService.saveBusinessCard(
                userDetails.getUserId(), requestDto);
        return ResponseEntity.ok(savedCard);
    }

    /**
     * QR 코드 URL 생성
     */
    @GetMapping("/qr")
    public ResponseEntity<Map<String, String>> generateQRCode(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String qrUrl = businessCardService.generateQRCode(userDetails.getUserId());
        return ResponseEntity.ok(Map.of("qrUrl", qrUrl));
    }

    /**
     * 공개 전자명함 조회 (QR 스캔 시 사용)
     */
    @GetMapping("/public/{userId}")
    public ResponseEntity<BusinessCardResponseDto> getPublicBusinessCard(
            @PathVariable Long userId) {
        BusinessCardResponseDto card = businessCardService.getPublicBusinessCard(userId);
        return ResponseEntity.ok(card);
    }

    /**
     * 전자명함 수집
     */
    @PostMapping("/collect/{cardOwnerId}")
    public ResponseEntity<String> collectBusinessCard(
            @PathVariable Long cardOwnerId,
            @RequestBody(required = false) Map<String, String> requestBody,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String memo = requestBody != null ? requestBody.get("memo") : null;
        businessCardService.collectBusinessCard(userDetails.getUserId(), cardOwnerId, memo);
        return ResponseEntity.ok("명함이 성공적으로 수집되었습니다.");
    }

    /**
     * 인코딩된 사용자 ID로 전자명함 수집
     */
    @PostMapping("/collect/encoded/{encodedUserId}")
    public ResponseEntity<String> collectBusinessCardByEncodedId(
            @PathVariable String encodedUserId,
            @RequestBody(required = false) Map<String, String> requestBody,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long cardOwnerId = qrService.decodeUserId(encodedUserId);
        String memo = requestBody != null ? requestBody.get("memo") : null;
        businessCardService.collectBusinessCard(userDetails.getUserId(), cardOwnerId, memo);
        return ResponseEntity.ok("명함이 성공적으로 수집되었습니다.");
    }

    /**
     * 수집한 명함 목록 조회
     */
    @GetMapping("/collected")
    public ResponseEntity<List<CollectedCardResponseDto>> getCollectedCards(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<CollectedCardResponseDto> collectedCards = 
                businessCardService.getCollectedCards(userDetails.getUserId());
        return ResponseEntity.ok(collectedCards);
    }

    /**
     * 수집한 명함 삭제
     */
    @DeleteMapping("/collected/{collectedCardId}")
    public ResponseEntity<String> deleteCollectedCard(
            @PathVariable Long collectedCardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        businessCardService.deleteCollectedCard(userDetails.getUserId(), collectedCardId);
        return ResponseEntity.ok("명함이 삭제되었습니다.");
    }

    /**
     * 수집한 명함 메모 수정
     */
    @PatchMapping("/collected/{collectedCardId}/memo")
    public ResponseEntity<String> updateCollectedCardMemo(
            @PathVariable Long collectedCardId,
            @RequestBody Map<String, String> requestBody,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String memo = requestBody.get("memo");
        businessCardService.updateCollectedCardMemo(userDetails.getUserId(), collectedCardId, memo);
        return ResponseEntity.ok("메모가 수정되었습니다.");
    }
}