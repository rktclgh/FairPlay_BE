package com.fairing.fairplay.file.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.file.dto.S3UploadRequestDto;
import com.fairing.fairplay.file.entity.File;
import com.fairing.fairplay.file.entity.FileLink;
import com.fairing.fairplay.file.repository.FileLinkRepository;
import com.fairing.fairplay.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final FileLinkRepository fileLinkRepository;
    private final AwsS3Service awsS3Service;

    @Transactional
    public File uploadFile(S3UploadRequestDto requestDto) {
        String directory = requestDto.getDirectoryPrefix();
        String newKey = awsS3Service.moveToPermanent(requestDto.getS3Key(), directory);
        log.info("FileService: Uploaded file to newKey: {}", newKey);

        File file = File.builder()
                .fileUrl(newKey)
                .referenced(true)
                .fileType(requestDto.getFileType())
                .directory(directory)
                .originalFileName(requestDto.getOriginalFileName())
                .storedFileName(extractFileName(newKey))
                .fileSize(requestDto.getFileSize())
                .thumbnail(requestDto.getUsage() != null && requestDto.getUsage().equalsIgnoreCase("thumbnail"))
                .build();

        return fileRepository.save(file);
    }

    @Transactional
    public void createFileLink(File file, String targetType, Long targetId) {
        FileLink fileLink = FileLink.builder()
                .file(file)
                .targetType(targetType)
                .targetId(targetId)
                .build();
        fileLinkRepository.save(fileLink);
        log.info("FileLink created: fileId={}, targetType={}, targetId={}", file.getId(), targetType, targetId);
    }

    @Transactional
    public void deleteFile(Long fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."));

        // 연관된 FileLink들 먼저 삭제
        List<FileLink> fileLinks = fileLinkRepository.findByFileId(fileId);
        fileLinkRepository.deleteAll(fileLinks);

        awsS3Service.deleteFile(file.getFileUrl());
        fileRepository.delete(file);
    }

    @Transactional
    public void deleteFileByS3Key(String s3Key) {
        File file = fileRepository.findByFileUrlContaining(s3Key)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."));

        // 연관된 FileLink들 먼저 삭제
        List<FileLink> fileLinks = fileLinkRepository.findByFileId(file.getId());
        fileLinkRepository.deleteAll(fileLinks);

        awsS3Service.deleteFile(file.getFileUrl());
        fileRepository.delete(file);
    }

    @Transactional
    public String moveFileToEvent(String currentS3Key, Long eventId, String usage) {
        // 현재 파일 찾기
        File currentFile = fileRepository.findByFileUrlContaining(currentS3Key)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "이동할 파일을 찾을 수 없습니다."));
        
        // S3에서 파일 존재 여부 확인
        try {
            if (!awsS3Service.fileExists(currentS3Key)) {
                log.warn("S3 파일이 존재하지 않음 - Key: {}, 파일 이동 건너뜀", currentS3Key);
                // 파일이 이미 이동되었거나 삭제된 경우, CDN URL 반환
                return awsS3Service.getCdnUrl(currentS3Key);
            }
        } catch (Exception e) {
            log.error("S3 파일 존재 확인 실패 - Key: {}, 오류: {}", currentS3Key, e.getMessage());
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "파일 존재 확인 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        // 새 경로 생성 (events/{eventId}/{usage})
        String newDirectoryPrefix = "events/" + eventId + "/" + usage;
        String newS3Key = awsS3Service.moveToPermanent(currentS3Key, newDirectoryPrefix);
        
        // 새로운 File 엔티티 생성 (기존 정보 복사 + 새 정보 설정)
        File newFile = File.builder()
                .fileUrl(newS3Key)
                .referenced(currentFile.isReferenced())
                .fileType(currentFile.getFileType())
                .directory(newDirectoryPrefix)
                .originalFileName(currentFile.getOriginalFileName())
                .storedFileName(extractFileName(newS3Key))
                .fileSize(currentFile.getFileSize())
                .thumbnail(currentFile.isThumbnail())
                .build();
        
        // 기존 FileLink들 삭제
        List<FileLink> oldFileLinks = fileLinkRepository.findByFileId(currentFile.getId());
        fileLinkRepository.deleteAll(oldFileLinks);
        
        // 기존 File 엔티티 삭제
        fileRepository.delete(currentFile);
        
        // 새 File 엔티티 저장
        File savedNewFile = fileRepository.save(newFile);
        
        // 새로운 FileLink 생성 (Event 연결)
        createFileLink(savedNewFile, "EVENT", eventId);
        
        log.info("파일 이동 완료 - EventId: {}, 기존 Key: {}, 새 Key: {}", eventId, currentS3Key, newS3Key);
        return awsS3Service.getCdnUrl(newS3Key);
    }

    @Transactional
    public String moveFileToBooth(String currentS3Key, Long eventId, Long boothId, String usage) {
        // 현재 파일 찾기
        File currentFile = fileRepository.findByFileUrlContaining(currentS3Key)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "이동할 파일을 찾을 수 없습니다."));

        // S3에서 파일 존재 여부 확인
        try {
            if (!awsS3Service.fileExists(currentS3Key)) {
                log.warn("S3 파일이 존재하지 않음 - Key: {}, 파일 이동 건너뜀", currentS3Key);
                // 파일이 이미 이동되었거나 삭제된 경우, CDN URL 반환
                return awsS3Service.getCdnUrl(currentS3Key);
            }
        } catch (Exception e) {
            log.error("S3 파일 존재 확인 실패 - Key: {}, 오류: {}", currentS3Key, e.getMessage());
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "파일 존재 확인 중 오류가 발생했습니다: " + e.getMessage());
        }

        // 새 경로 생성
        String newDirectoryPrefix = "events/" + eventId + "/booths/" + boothId + "/" + usage;
        String newS3Key = awsS3Service.moveToPermanent(currentS3Key, newDirectoryPrefix);

        // 새로운 File 엔티티 생성 (기존 정보 복사 + 새 정보 설정)
        File newFile = File.builder()
                .fileUrl(newS3Key)
                .referenced(currentFile.isReferenced())
                .fileType(currentFile.getFileType())
                .directory(newDirectoryPrefix)
                .originalFileName(currentFile.getOriginalFileName())
                .storedFileName(extractFileName(newS3Key))
                .fileSize(currentFile.getFileSize())
                .thumbnail(currentFile.isThumbnail())
                .build();

        // 기존 FileLink들 삭제
        List<FileLink> oldFileLinks = fileLinkRepository.findByFileId(currentFile.getId());
        fileLinkRepository.deleteAll(oldFileLinks);

        // 기존 File 엔티티 삭제
        fileRepository.delete(currentFile);

        // 새 File 엔티티 저장
        File savedNewFile = fileRepository.save(newFile);

        // 새로운 FileLink 생성 (Event 연결)
        createFileLink(savedNewFile, "BOOTH", eventId);

        log.info("파일 이동 완료 - 기존 Key: {}, 새 Key: {}", currentS3Key, newS3Key);
        return awsS3Service.getCdnUrl(newS3Key);
    }

    private String extractFileName(String key) {
        return key.substring(key.lastIndexOf('/') + 1);
    }
}
