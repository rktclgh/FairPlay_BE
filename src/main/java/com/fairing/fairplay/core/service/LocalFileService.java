package com.fairing.fairplay.core.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.dto.FileUploadResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileService {

    @Value("${app.base-url:https://fair-play.ink}")
    private String baseUrl;

    @Value("${app.upload.path:${user.home}/fairplay-uploads}")
    private String uploadBasePath;

    // 파일 임시 저장
    public FileUploadResponseDto uploadTemp(MultipartFile file) {
        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf('.')))
                .orElse("");
        String uuid = UUID.randomUUID().toString();
        String key = "uploads/tmp" + LocalDate.now() + "/" + uuid + ext;
        
        Path filePath = Paths.get(uploadBasePath, key);
        
        try {
            // 디렉토리 생성
            Files.createDirectories(filePath.getParent());
            
            // 파일 저장
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("로컬 파일 업로드 실패. Key: {}", key, e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다: " + e.getMessage());
        }

        log.info("Temporary file uploaded successfully - Key: {}, Original: {}, Size: {}",
                key, file.getOriginalFilename(), file.getSize());

        // 미리보기용 URL (상대 경로로 반환, 프론트엔드에서 baseUrl 추가)
        String downloadUrl = "/api/uploads/download?key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);

        // 이미지 여부
        boolean isImage = file.getContentType() != null && file.getContentType().startsWith("image/");

        return new FileUploadResponseDto(key, downloadUrl, file.getOriginalFilename(), file.getContentType(), isImage);
    }

    // 파일 저장
    public String moveToPermanent(String key, String destPrefix) {
        if (key == null || key.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "파일 키가 유효하지 않습니다.");
        }
        
        String ext = key.contains(".") ? key.substring(key.lastIndexOf('.')) : "";
        String uuid = UUID.randomUUID().toString();
        String destKey = "uploads/" + destPrefix + "/" + uuid + ext;

        Path sourcePath = Paths.get(uploadBasePath, key);
        Path destPath = Paths.get(uploadBasePath, destKey);

        try {
            // 원본 파일 존재 확인
            if (!Files.exists(sourcePath)) {
                throw new CustomException(HttpStatus.NOT_FOUND, "영구 저장으로 이동할 임시 파일을 찾을 수 없습니다: " + key);
            }

            // 대상 디렉토리 생성
            Files.createDirectories(destPath.getParent());
            
            // 파일 이동
            Files.move(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Successfully moved file from {} to {}", key, destKey);
            return destKey;

        } catch (IOException e) {
            log.error("Error moving file from {} to {}: {}", key, destKey, e.getMessage(), e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 이동 중 오류가 발생했습니다.");
        }
    }

    // 파일 다운로드
    public void downloadFile(String key, HttpServletResponse response) throws IOException {
        Path filePath = Paths.get(uploadBasePath, key);
        
        if (!Files.exists(filePath)) {
            log.error("로컬 파일 다운로드 실패. 파일을 찾을 수 없음. Key: {}", key);
            throw new CustomException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");
        }

        try {
            String contentType = Files.probeContentType(filePath);
            response.setContentType(contentType != null ? contentType : "application/octet-stream");
            
            String fileName = key != null && key.contains("/") ? 
                key.substring(key.lastIndexOf('/') + 1) : 
                (key != null ? key : "download");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"");

            try (InputStream inputStream = Files.newInputStream(filePath)) {
                IOUtils.copy(inputStream, response.getOutputStream());
            }
            response.flushBuffer();
        } catch (IOException e) {
            log.error("로컬 파일 다운로드 실패. Key: {}", key, e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 다운로드 중 오류가 발생했습니다.");
        }
    }

    public String getPublicUrl(String key) {
        return baseUrl + "/uploads/" + key;
    }

    /**
     * Static 파일 서빙을 위한 URL 생성
     */
    public String getCdnUrl(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        
        String cleanKey = key.startsWith("/") ? key.substring(1) : key;
        return baseUrl + "/uploads/" + cleanKey;
    }

    public String getStaticKeyFromPublicUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return null;
        }
        
        try {
            java.net.URI uri = java.net.URI.create(publicUrl.trim());
            String path = uri.getPath();
            
            if (path != null && path.startsWith("/uploads/")) {
                String key = path.substring("/uploads/".length());
                return java.net.URLDecoder.decode(key, java.nio.charset.StandardCharsets.UTF_8);
            }
            
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // 파일 삭제
    public void deleteFile(String key) {
        Path filePath = Paths.get(uploadBasePath, key);
        
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("로컬 파일 삭제 성공. Key: {}", key);
            } else {
                log.warn("삭제하려는 파일이 존재하지 않음. Key: {}", key);
            }
        } catch (IOException e) {
            log.error("로컬 파일 삭제 실패. Key: {}", key, e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제 중 오류가 발생했습니다.");
        }
    }

    /**
     * 로컬 파일 시스템에서 파일 존재 여부 확인
     */
    public boolean fileExists(String key) {
        Path filePath = Paths.get(uploadBasePath, key);
        return Files.exists(filePath);
    }
}