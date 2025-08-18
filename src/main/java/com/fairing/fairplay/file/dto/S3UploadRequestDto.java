package com.fairing.fairplay.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class S3UploadRequestDto {
    private String s3Key;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String directoryPrefix; // S3 저장 경로 접두사
    private String usage; // 파일 사용 용도 (application_file, banner, thumbnail 등)
}
