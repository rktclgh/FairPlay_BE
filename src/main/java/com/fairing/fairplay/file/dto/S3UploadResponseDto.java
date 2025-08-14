package com.fairing.fairplay.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class S3UploadResponseDto {
    private Long fileId;
    private String fileUrl; // CloudFront URL이 설정된 경우 CDN URL, 그렇지 않으면 직접 S3 URL
    
    /**
     * CloudFront CDN을 통한 공개 URL 반환
     * @deprecated Use getFileUrl() instead
     */
    @Deprecated
    public String getPublicUrl() {
        return fileUrl;
    }
}
