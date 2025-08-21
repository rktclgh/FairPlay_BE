package com.fairing.fairplay.businesscard.dto;

import com.fairing.fairplay.businesscard.entity.CollectedBusinessCard;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CollectedCardResponseDto {
    private Long id;
    private Long cardOwnerId;
    private BusinessCardResponseDto businessCard;
    private String memo;
    private LocalDateTime collectedAt;
    
    public static CollectedCardResponseDto from(CollectedBusinessCard collected) {
        BusinessCardResponseDto cardDto = null;
        
        // 카드 소유자의 명함 정보 조회
        if (collected.getCardOwner().getBusinessCard() != null) {
            cardDto = BusinessCardResponseDto.from(collected.getCardOwner().getBusinessCard())
                    .filterEmptyFields(); // 비어있는 필드 제외
        }
        
        return CollectedCardResponseDto.builder()
                .id(collected.getId())
                .cardOwnerId(collected.getCardOwner().getUserId())
                .businessCard(cardDto)
                .memo(collected.getMemo())
                .collectedAt(collected.getCollectedAt())
                .build();
    }
}