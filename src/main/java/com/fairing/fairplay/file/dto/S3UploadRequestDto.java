package com.fairing.fairplay.file.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class S3UploadRequestDto {
    private String s3Key;
    private Long eventId;
    private Long eventApplyId;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
}
