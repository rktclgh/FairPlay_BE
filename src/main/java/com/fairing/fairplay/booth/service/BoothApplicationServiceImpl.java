package com.fairing.fairplay.booth.service;

import com.fairing.fairplay.booth.dto.*;
import com.fairing.fairplay.booth.entity.*;
import com.fairing.fairplay.booth.mapper.BoothApplicationMapper;
import com.fairing.fairplay.booth.repository.*;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.repository.EventRepository;
import com.fairing.fairplay.file.dto.S3UploadRequestDto;
import com.fairing.fairplay.file.dto.TempFileUploadDto;
import com.fairing.fairplay.file.entity.File;
import com.fairing.fairplay.file.service.FileService;
import com.fairing.fairplay.user.entity.BoothAdmin;
import com.fairing.fairplay.user.entity.UserRoleCode;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.user.repository.UserRoleCodeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BoothApplicationServiceImpl implements BoothApplicationService {

    private final BoothApplicationRepository boothApplicationRepository;
    private final EventRepository eventRepository;
    private final BoothApplicationStatusCodeRepository statusCodeRepository;
    private final BoothPaymentStatusCodeRepository paymentCodeRepository;
    private final BoothApplicationMapper mapper;
    private final UserRepository userRepository;
    private final UserRoleCodeRepository userRoleCodeRepository;
    private final BoothRepository boothRepository;
    private final BoothTypeRepository boothTypeRepository;
    private final BoothAdminRepository boothAdminRepository;
    private final AwsS3Service awsS3Service;
    private final FileService fileService;

    @Override
    public Long applyBooth(BoothApplicationRequestDto dto) {

        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다.");
        }

        Event event = eventRepository.findById(dto.getEventId())
                .orElseThrow(() -> new EntityNotFoundException("행사를 찾을 수 없습니다."));

        BoothApplicationStatusCode status = statusCodeRepository.findByCode("PENDING")
                .orElseThrow(() -> new EntityNotFoundException("상태 코드 없음"));

        BoothPaymentStatusCode paymentStatus = paymentCodeRepository.findByCode("PENDING")
                .orElseThrow(() -> new EntityNotFoundException("결제 상태 코드 없음"));

        BoothType boothType = boothTypeRepository.findById(dto.getBoothTypeId())
                .orElseThrow(() -> new EntityNotFoundException("선택한 부스 타입을 찾을 수 없습니다."));

        BoothApplication boothApplication = mapper.toEntity(dto, event, boothType, status, paymentStatus);

        BoothApplication saved = boothApplicationRepository.save(boothApplication);

        if (dto.getTempBannerUrl() != null) {
            processAndLinkFiles(saved, dto.getTempBannerUrl());
        }

        return saved.getId();
    }

    private void processAndLinkFiles(BoothApplication boothApply, TempFileUploadDto tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            String directory = "booth-apply/" + tempFile.getUsage();

            File savedFile = fileService.uploadFile(S3UploadRequestDto.builder()
                    .s3Key(tempFile.getS3Key())
                    .originalFileName(tempFile.getOriginalFileName())
                    .fileType(tempFile.getFileType())
                    .fileSize(tempFile.getFileSize())
                    .directoryPrefix(directory)
                    .usage(tempFile.getUsage())
                    .build());

            fileService.createFileLink(savedFile, "BOOTH_APPLY", boothApply.getId());

            String cdnUrl = awsS3Service.getCdnUrl(savedFile.getFileUrl());
            boothApply.setBoothBannerUrl(cdnUrl);

        } catch (Exception e) {
            log.error("Error processing temporary file with key {}: {}", tempFile.getS3Key(), e.getMessage(), e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 처리 중 오류가 발생했습니다: " + tempFile.getOriginalFileName());
        }
    }

    @Override
    public List<BoothApplicationListDto> getBoothApplications(Long eventId) {
        return boothApplicationRepository.findByEvent_EventId(eventId).stream()
                .map(mapper::toListDto)
                .toList();
    }

    @Override
    public BoothApplicationResponseDto getBoothApplication(Long id) {
        BoothApplication application = boothApplicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("신청 정보를 찾을 수 없습니다."));
        return mapper.toResponseDto(application);
    }

    @Override
    public void updateStatus(Long id, BoothApplicationStatusUpdateDto dto) {
        BoothApplication application = boothApplicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("신청 정보를 찾을 수 없습니다."));

        BoothApplicationStatusCode newStatus = statusCodeRepository.findByCode(dto.getStatusCode())
                .orElseThrow(() -> new EntityNotFoundException("상태 코드가 유효하지 않습니다."));

        application.setBoothApplicationStatusCode(newStatus);
        application.setAdminComment(dto.getAdminComment());
        application.setStatusUpdatedAt(LocalDateTime.now());

        if ("APPROVED".equals(newStatus.getCode())) {
            BoothType boothType = application.getBoothType();

            BoothAdmin boothAdmin = boothAdminRepository.findByEmail(application.getContactEmail())
                    .orElseGet(() -> {
                        Users user = userRepository.findByEmail(application.getContactEmail())
                                .orElseThrow(() -> new EntityNotFoundException("사용자 없음"));

                        BoothAdmin newAdmin = new BoothAdmin();
                        newAdmin.setUser(user);
                        newAdmin.setEmail(application.getContactEmail());
                        newAdmin.setManagerName(application.getManagerName());
                        newAdmin.setContactNumber(application.getContactNumber());
                        newAdmin.setOfficialUrl(application.getOfficialUrl());

                        return boothAdminRepository.save(newAdmin);
                    });

            Booth booth = new Booth();
            booth.setEvent(application.getEvent());
            booth.setBoothTitle(application.getBoothTitle());
            booth.setBoothDescription(application.getBoothDescription());
            booth.setStartDate(application.getStartDate());
            booth.setEndDate(application.getEndDate());
            booth.setLocation("위치 미정");
            booth.setBoothType(boothType);
            booth.setBoothAdmin(boothAdmin);

            boothRepository.save(booth);
        }
    }

    @Override
    public void updatePaymentStatus(Long id, BoothPaymentStatusUpdateDto dto) {
        BoothApplication booth = boothApplicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("부스 신청 정보를 찾을 수 없습니다."));

        BoothPaymentStatusCode statusCode = paymentCodeRepository
                .findByCode(dto.getPaymentStatusCode())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 결제 상태 코드입니다."));

        booth.setBoothPaymentStatusCode(statusCode);
        booth.setAdminComment(dto.getAdminComment());
        booth.setStatusUpdatedAt(LocalDateTime.now());

        if ("PAID".equals(dto.getPaymentStatusCode())) {
            Users user = userRepository.findByEmail(booth.getContactEmail())
                    .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

            UserRoleCode boothManagerCode = userRoleCodeRepository.findByCode("BOOTH_MANAGER")
                    .orElseThrow(() -> new EntityNotFoundException("BOOTH_MANAGER 권한 코드가 존재하지 않습니다."));

            user.setRoleCode(boothManagerCode);
        }

    }

    @Override
    public void cancelApplication(Long id, Long userId) {
        BoothApplication application = boothApplicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("부스 신청 정보를 찾을 수 없습니다."));

        String requesterEmail = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."))
                .getEmail();

        if (!application.getContactEmail().equalsIgnoreCase(requesterEmail)) {
            throw new SecurityException("해당 신청을 취소할 권한이 없습니다.");
        }

        BoothPaymentStatusCode cancelled = paymentCodeRepository.findByCode("CANCELLED")
                .orElseThrow(() -> new EntityNotFoundException("결제 상태 코드(CANCELLED)를 찾을 수 없습니다."));

        application.setBoothPaymentStatusCode(cancelled);
        application.setStatusUpdatedAt(LocalDateTime.now());
    }
}