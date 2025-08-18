package com.fairing.fairplay.booth.dto;

import com.fairing.fairplay.booth.entity.Booth;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothResponseDto {

    private Long boothId; // 부스 ID
    
    private String boothTitle; // 부스 제목
    
    private String boothDescription; // 부스 설명
    
    private Long eventId; // 행사 ID
    
    private String eventTitle; // 행사 제목
    
    private Long boothAdminId; // 부스 관리자 ID
    
    private String boothAdminName; // 부스 관리자 이름

    // 엔티티에서 DTO로 변환
    public static BoothResponseDto fromEntity(Booth entity) {
        return BoothResponseDto.builder()
                .boothId(entity.getId())
                .boothTitle(entity.getBoothTitle())
                .boothDescription(entity.getBoothDescription())
                .eventId(entity.getEvent().getEventId())
                .eventTitle(entity.getEvent().getTitleKr())
                .boothAdminId(entity.getBoothAdmin() != null ? entity.getBoothAdmin().getUserId() : null)
                .boothAdminName(entity.getBoothAdmin() != null && entity.getBoothAdmin().getUser() != null ? 
                    entity.getBoothAdmin().getUser().getName() : null)
                .build();
    }
}