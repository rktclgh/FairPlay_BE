package com.fairing.fairplay.banner.dto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NewPickDto {
    private Long id;               // eventId (프론트에서 event.id로 매칭)
    private String title;
    private String image;          // 썸네일
    private String date;           // "YYYY-MM-DD ~ YYYY-MM-DD"
    private String location;
    private String category;
    private LocalDateTime createdAt; // NEW 판별 기준
}
