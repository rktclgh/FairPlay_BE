package com.fairing.fairplay.event.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStatusThumbnailDto {
    private Boolean hidden;
        private String thumbnailUrl;
    private FileUploadDto thumbnailFile;

    @Getter
    @Setter
    public static class FileUploadDto {
        private String s3Key;
        private String originalFileName;
        private String fileType;
        private Long fileSize;
    }
}
