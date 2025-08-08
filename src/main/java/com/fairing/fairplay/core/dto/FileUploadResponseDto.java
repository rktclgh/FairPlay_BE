package com.fairing.fairplay.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileUploadResponseDto {
    private String key;     // S3 key (temp 경로)
    private String url;     // 프론트 미리보기용 (백엔드 프록시 다운로드 URL
    private String name;    // 원본 파일명(표시용)
    private String type;    // MIME 타입
    private boolean isImage;
}
