package com.fairing.fairplay.file.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private final FileRepository fileRepository;
    private final AwsS3Service awsS3Service;
    private final EventRepository eventRepository;
    private final EventApplyRepository eventApplyRepository;

    @Transactional
    public S3UploadResponseDto uploadFile(S3UploadRequestDto requestDto) {
        String directory = determineDirectory(requestDto.getEventId(), requestDto.getEventApplyId());
        String newKey = awsS3Service.moveToPermanent(requestDto.getS3Key(), directory);

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

    private String determineDirectory(Long eventId, Long eventApplyId) {
        if (eventId != null) {
            return "events/" + eventId;
        } else if (eventApplyId != null) {
            return "event-applies/" + eventApplyId;
        } else {
            return "etc";
        }
    }

    private String extractFileName(String key) {
        return key.substring(key.lastIndexOf('/') + 1);
    }
}
