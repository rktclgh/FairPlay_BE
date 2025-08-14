package com.fairing.fairplay.file.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventApply;
import com.fairing.fairplay.event.repository.EventApplyRepository;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.file.dto.S3UploadRequestDto;
import com.fairing.fairplay.file.dto.S3UploadResponseDto;
import com.fairing.fairplay.file.entity.File;
import com.fairing.fairplay.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final AwsS3Service awsS3Service;
    private final EventRepository eventRepository;
    private final EventApplyRepository eventApplyRepository;

    @Transactional
    public S3UploadResponseDto uploadFile(S3UploadRequestDto requestDto) {
        String directory = requestDto.getDirectoryPrefix();
        String newKey = awsS3Service.moveToPermanent(requestDto.getS3Key(), directory);
        log.info("FileService: Uploaded file to newKey: {}", newKey);

        Event event = requestDto.getEventId() != null ? eventRepository.findById(requestDto.getEventId()).orElse(null) : null;
        EventApply eventApply = requestDto.getEventApplyId() != null ? eventApplyRepository.findById(requestDto.getEventApplyId()).orElse(null) : null;

        File file = File.builder()
                .event(event)
                .eventApply(eventApply)
                .fileUrl(newKey)
                .referenced(true)
                .fileType(requestDto.getFileType())
                .directory(directory)
                .originalFileName(requestDto.getOriginalFileName())
                .storedFileName(extractFileName(newKey))
                .fileSize(requestDto.getFileSize())
                .thumbnail(false)
                .build();

        File savedFile = fileRepository.save(file);

        return S3UploadResponseDto.builder()
                .fileId(savedFile.getId())
                .fileUrl(awsS3Service.getCdnUrl(newKey))
                .build();
    }

    @Transactional
    public void deleteFile(Long fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."));

        awsS3Service.deleteFile(file.getFileUrl());
        fileRepository.delete(file);
    }

    @Transactional
    public void deleteFileByS3Key(String s3Key) {
        File file = fileRepository.findByFileUrlContaining(s3Key)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."));

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
        
        // Event 엔티티 조회
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."));
        
        // 새로운 File 엔티티 생성 (기존 정보 복사 + 새 정보 설정)
        File newFile = File.builder()
                .event(event)
                .eventApply(null) // EventApply 연결 해제
                .fileUrl(newS3Key)
                .referenced(currentFile.isReferenced())
                .fileType(currentFile.getFileType())
                .directory(newDirectoryPrefix)
                .originalFileName(currentFile.getOriginalFileName())
                .storedFileName(extractFileName(newS3Key))
                .fileSize(currentFile.getFileSize())
                .thumbnail(currentFile.isThumbnail())
                .build();
        
        // 기존 File 엔티티 삭제
        fileRepository.delete(currentFile);
        
        // 새 File 엔티티 저장
        fileRepository.save(newFile);
        
        log.info("파일 이동 완료 - EventId: {}, 기존 Key: {}, 새 Key: {}", eventId, currentS3Key, newS3Key);
        return awsS3Service.getCdnUrl(newS3Key);
    }

    private String extractFileName(String key) {
        return key.substring(key.lastIndexOf('/') + 1);
    }
}
