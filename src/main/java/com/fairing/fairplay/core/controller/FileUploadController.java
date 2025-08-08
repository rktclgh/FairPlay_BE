package com.fairing.fairplay.core.controller;

import com.fairing.fairplay.core.dto.FileUploadResponseDto;
import com.fairing.fairplay.core.service.AwsS3Service;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/uploads")
public class FileUploadController {

    private final AwsS3Service awsS3Service;

    // 임시 업로드 (파일/이미지)
    @PostMapping("/temp")
    public FileUploadResponseDto uploadTemp(@RequestParam MultipartFile file) throws IOException {
        return awsS3Service.uploadTemp(file);
    }

    // 다운로드
    @GetMapping("/download")
    public void download (@RequestParam String key, HttpServletResponse response) throws IOException {
        awsS3Service.downloadFile(key, response);
    }

}
