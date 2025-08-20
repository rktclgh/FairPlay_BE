package com.fairing.fairplay.event.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventStatusThumbnailDto {
    private Boolean hidden;
    private String thumbnailUrl;
    private FileUploadDto thumbnailFile;
    
    // 프론트엔드에서 보내지만 무시할 필드들
    private String email;
    private String titleKr;
    private String titleEng;

    @Getter
    @Setter
    public static class FileUploadDto {
        private String s3Key;
        private String originalFileName;
        private String fileType;
        private Long fileSize;
    }
}
