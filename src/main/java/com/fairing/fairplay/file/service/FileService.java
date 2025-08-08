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
                .fileUrl(awsS3Service.getPublicUrl(newKey))
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

    private String extractFileName(String key) {
        return key.substring(key.lastIndexOf('/') + 1);
    }
}
