package com.fairing.fairplay.booth.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoothExperienceStatusUpdateDto {

    private String statusCode; // 변경할 상태 코드 (WAITING, READY, IN_PROGRESS, COMPLETED, CANCELLED)

    private String notes; // 상태 변경 사유 또는 메모 (선택사항)
}