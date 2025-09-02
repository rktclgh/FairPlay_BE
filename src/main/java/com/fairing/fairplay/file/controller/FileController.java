package com.fairing.fairplay.file.controller;

import com.fairing.fairplay.core.service.LocalFileService;
// import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.file.dto.S3UploadRequestDto;
import com.fairing.fairplay.file.dto.S3UploadResponseDto;
import com.fairing.fairplay.file.entity.File;
import com.fairing.fairplay.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    // private final AwsS3Service awsS3Service;
    private final LocalFileService localFileService;

    @PostMapping
    public ResponseEntity<S3UploadResponseDto> uploadFile(@RequestBody S3UploadRequestDto requestDto) {
        File savedFile = fileService.uploadFile(requestDto);
        S3UploadResponseDto responseDto = S3UploadResponseDto.builder()
                .fileId(savedFile.getId())
                .fileUrl(localFileService.getCdnUrl(savedFile.getFileUrl()))
                .build();
                
        /* S3 버전 (롤백용 주석처리)
        S3UploadResponseDto responseDto = S3UploadResponseDto.builder()
                .fileId(savedFile.getId())
                .fileUrl(awsS3Service.getCdnUrl(savedFile.getFileUrl()))
                .build();
        */
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long fileId) {
        fileService.deleteFile(fileId);
        return ResponseEntity.noContent().build();
    }

}