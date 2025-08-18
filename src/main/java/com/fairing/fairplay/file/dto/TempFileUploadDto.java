package com.fairing.fairplay.file.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TempFileUploadDto {
    private String s3Key;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String usage; // "application_file", "banner", "thumbnail" 등
}
