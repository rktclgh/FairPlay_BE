package com.fairing.fairplay.booth.service;

import com.fairing.fairplay.booth.dto.BoothUpdateRequestDto;
import com.fairing.fairplay.booth.entity.Booth;
import com.fairing.fairplay.booth.repository.BoothRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.file.dto.S3UploadRequestDto;
import com.fairing.fairplay.file.entity.File;
import com.fairing.fairplay.file.service.FileService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BoothServiceImpl implements BoothService {

    private final BoothRepository boothRepository;
    private final FileService fileService;
    private final AwsS3Service awsS3Service;

    @Override
    public void updateBooth(Long boothId, BoothUpdateRequestDto dto) {
        Booth booth = boothRepository.findById(boothId)
                .orElseThrow(() -> new EntityNotFoundException("부스를 찾을 수 없습니다: " + boothId));

        // Update booth fields
        if (dto.getBoothTitle() != null) {
            booth.setBoothTitle(dto.getBoothTitle());
        }
        if (dto.getBoothDescription() != null) {
            booth.setBoothDescription(dto.getBoothDescription());
        }
        if (dto.getStartDate() != null) {
            booth.setStartDate(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            booth.setEndDate(dto.getEndDate());
        }
        if (dto.getLocation() != null) {
            booth.setLocation(dto.getLocation());
        }

        // Handle file deletions
        if (dto.getDeletedFileIds() != null && !dto.getDeletedFileIds().isEmpty()) {
            for (Long fileId : dto.getDeletedFileIds()) {
                fileService.deleteFile(fileId);
            }
        }

        // Handle file uploads
        if (dto.getTempFiles() != null && !dto.getTempFiles().isEmpty()) {
            dto.getTempFiles().forEach(tempFile -> {
                String directory = "booth/" + boothId + "/" + tempFile.getUsage();
                File savedFile = fileService.uploadFile(S3UploadRequestDto.builder()
                        .s3Key(tempFile.getS3Key())
                        .originalFileName(tempFile.getOriginalFileName())
                        .fileType(tempFile.getFileType())
                        .fileSize(tempFile.getFileSize())
                        .directoryPrefix(directory)
                        .usage(tempFile.getUsage())
                        .build());

                fileService.createFileLink(savedFile, "BOOTH", boothId);
            });
        }

        boothRepository.save(booth);
    }
}
