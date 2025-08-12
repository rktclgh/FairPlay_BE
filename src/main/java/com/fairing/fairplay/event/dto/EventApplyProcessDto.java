package com.fairing.fairplay.event.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventApplyProcessDto {
    
    private String action; // "approve" or "reject"
    private String adminComment; // 승인/반려 시 관리자 코멘트
}