package com.fairing.fairplay.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class S3UploadResponseDto {
    private Long fileId;
    private String fileUrl;
}
