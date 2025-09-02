package com.fairing.fairplay.core.controller;

import com.fairing.fairplay.core.dto.FileUploadResponseDto;
import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.core.service.LocalFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/uploads")
public class FileUploadController {

    // S3 서비스 (주석처리 - 필요시 롤백 가능)
    // private final AwsS3Service awsS3Service;
    
    // 로컬 파일 서비스
    private final LocalFileService localFileService;

    // 임시 업로드 (파일/이미지)
    @PostMapping("/temp")
    public FileUploadResponseDto uploadTemp(@RequestParam MultipartFile file) throws IOException {
        log.info("파일 업로드 요청 - 파일명: {}, 크기: {} bytes, 타입: {}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType());
        
        if (file.isEmpty()) {
            log.error("빈 파일 업로드 시도 - 파일명: {}", file.getOriginalFilename());
            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }
        
        // S3 업로드 (주석처리)
        // return awsS3Service.uploadTemp(file);
        
        // 로컬 파일 업로드
        return localFileService.uploadTemp(file);
    }

    // 다운로드
    @GetMapping("/download")
    public void download (@RequestParam String key, HttpServletResponse response) throws IOException {
        // S3 다운로드 (주석처리)
        // awsS3Service.downloadFile(key, response);
        
        // 로컬 파일 다운로드
        localFileService.downloadFile(key, response);
    }

}
