package com.fairing.fairplay.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.Email;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequestDto {  // 행사 기본 등록 요청

    @Email
    private String email;       // 전체 관리자가 부여한 행사 관리자 이메일 -> 이메일로 관리자 등록 및 연결
    
    private String titleKr;     // 국문 행사명
    private String titleEng;    // 영문 행사명
    
}
