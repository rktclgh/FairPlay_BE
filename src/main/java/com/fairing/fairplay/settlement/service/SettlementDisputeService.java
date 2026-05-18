package com.fairing.fairplay.settlement.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.core.service.LocalFileService;
import com.fairing.fairplay.core.util.TempUploadKeyPolicy;
import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.settlement.controller.SettlementDisputeController;
import com.fairing.fairplay.settlement.dto.SettlementDisputeDto;
import com.fairing.fairplay.settlement.entity.DisputeStatus;
import com.fairing.fairplay.settlement.entity.Settlement;
import com.fairing.fairplay.settlement.entity.SettlementDispute;
import com.fairing.fairplay.settlement.entity.SettlementDisputeFile;
import com.fairing.fairplay.settlement.repository.SettlementDisputeFileRepository;
import com.fairing.fairplay.settlement.repository.SettlementDisputeRepository;
import com.fairing.fairplay.settlement.repository.SettlementRepository;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementDisputeService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EVENT_MANAGER = "EVENT_MANAGER";

    private final SettlementDisputeRepository disputeRepository;
    private final SettlementDisputeFileRepository disputeFileRepository;
    private final SettlementRepository settlementRepository;
    private final LocalFileService localFileService;
    private final UserRepository userRepository;

    /**
     * 이의신청용 파일 임시 업로드
     */
    public SettlementDisputeDto.TempUploadResponse uploadDisputeFiles(List<MultipartFile> files,
                                                                      CustomUserDetails userDetails) {
        requireEventManager(userDetails);

        if (files == null || files.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "업로드할 파일이 없습니다.");
        }

        if (files.size() > 10) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "최대 10개까지만 파일을 업로드할 수 있습니다.");
        }

        List<String> tempKeys = files.stream()
                .map(file -> {
                    // 파일 타입 검증
                    validateDisputeFile(file);
                    // 임시 업로드
                    return localFileService.uploadTemp(file).getKey();
                })
                .toList();

        return SettlementDisputeDto.TempUploadResponse.builder()
                .files(IntStream.range(0, tempKeys.size())
                        .mapToObj(i -> SettlementDisputeDto.TempUploadResponse.TempFileInfo.builder()
                                .originalFilename(files.get(i).getOriginalFilename())
                                .fileSize(files.get(i).getSize())
                                .fileType(SettlementDisputeFile.DisputeFileType.fromContentType(files.get(i).getContentType()))
                                .uploadOrder(i + 1)
                                .tempKey(tempKeys.get(i))
                                .build())
                        .toList())
                .build();
    }

    /**
     * 이의신청 제출
     */
    @Transactional
    public SettlementDisputeDto.Response submitDispute(SettlementDisputeDto.CreateRequest request,
                                                       CustomUserDetails userDetails) {
        // 정산 정보 조회
        Settlement settlement = settlementRepository.findById(request.getSettlementId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "정산 정보를 찾을 수 없습니다."));
        requireManagedSettlementAccess(settlement, userDetails);

        // 이미 이의신청이 있는지 확인
        if (disputeRepository.existsBySettlement_SettlementId(request.getSettlementId())) {
            throw new CustomException(HttpStatus.CONFLICT, "이미 이의신청이 접수된 정산입니다.");
        }

        // 임시 파일들이 실제로 존재하는지 확인
        if (request.getTempFileKeys() != null) {
            for (String key : request.getTempFileKeys()) {
                if (!TempUploadKeyPolicy.isTemporaryUploadKey(key) || !localFileService.fileExists(key)) {
                    throw new CustomException(HttpStatus.NOT_FOUND, "임시 업로드된 파일을 찾을 수 없습니다: " + key);
                }
            }
        }

        // 이의신청 생성
        SettlementDispute dispute = SettlementDispute.builder()
                .settlement(settlement)
                .requesterId(userDetails.getUserId())
                .requesterName(resolvePrincipalName(userDetails))
                .disputeReason(request.getDisputeReason())
                .status(SettlementDispute.DisputeProcessStatus.RAISED)
                .build();

        dispute = disputeRepository.save(dispute);

        // 임시 파일들을 영구 경로로 이동
        if (request.getTempFileKeys() != null && !request.getTempFileKeys().isEmpty()) {
            moveFilesToPermanent(dispute, request.getTempFileKeys());
        }

        // Settlement의 disputeStatus 업데이트
        settlement.setDisputeStatus(DisputeStatus.RAISED);
        settlementRepository.save(settlement);

        log.info("정산 이의신청 접수 완료 - DisputeId: {}, SettlementId: {}, RequesterId: {}",
                dispute.getDisputeId(), settlement.getSettlementId(), userDetails.getUserId());

        return convertToResponse(dispute);
    }

    /**
     * 임시 파일들을 영구 경로로 이동
     */
    private void moveFilesToPermanent(SettlementDispute dispute, List<String> tempKeys) {
        String destPrefix = "disputes/" + dispute.getDisputeId();

        for (int i = 0; i < tempKeys.size(); i++) {
            String tempKey = tempKeys.get(i);
            try {
                // 파일을 영구 경로로 이동
               String permanentKey = localFileService.moveToPermanent(tempKey, destPrefix);

                // 파일 정보를 DB에 저장
                String originalFilename = extractOriginalFilename(tempKey);
                SettlementDisputeFile disputeFile = SettlementDisputeFile.builder()
                        .dispute(dispute)
                        .s3Key(permanentKey)
                        .originalFilename(originalFilename)
                        .uploadOrder(i + 1)
                        .build();

                // 파일 타입과 크기는 S3에서 메타데이터를 가져와서 설정할 수도 있지만
                // 여기서는 파일명 확장자로 간단히 판단
                disputeFile.setFileType(determineFileType(originalFilename));

                disputeFileRepository.save(disputeFile);
            } catch (Exception e) {
                log.error("이의신청 파일 이동 실패 - TempKey: {}, DisputeId: {}", tempKey, dispute.getDisputeId(), e);
                throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 중 오류가 발생했습니다.");
            }
        }
    }

    /**
     * 파일명에서 확장자를 기반으로 파일 타입 결정
     */
    private SettlementDisputeFile.DisputeFileType determineFileType(String filename) {
        if (filename == null) return null;

        String ext = filename.toLowerCase();
        if (ext.endsWith(".pdf")) {
            return SettlementDisputeFile.DisputeFileType.PDF;
        } else if (ext.endsWith(".xlsx") || ext.endsWith(".xls")) {
            return SettlementDisputeFile.DisputeFileType.EXCEL;
        }
        return null;
    }

    /**
     * 임시 키에서 원본 파일명 추출
     */
    private String extractOriginalFilename(String s3Key) {
        // uploads/tmp2025-01-01/uuid.ext 형태에서 파일명 부분만 추출
        return s3Key.substring(s3Key.lastIndexOf('/') + 1);
    }

    /**
     * 이의신청 파일 유효성 검증
     */
    private void validateDisputeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "빈 파일은 업로드할 수 없습니다.");
        }

        // 파일 크기 제한 (50MB)
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "파일 크기는 50MB를 초과할 수 없습니다.");
        }

        // 허용된 파일 타입 검증
        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("application/pdf") &&
                        !contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") &&
                        !contentType.equals("application/vnd.ms-excel"))) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "PDF 또는 Excel 파일만 업로드 가능합니다.");
        }
    }

    /**
     * 이의신청 상세 조회
     */
    @Transactional(readOnly = true)
    public SettlementDisputeDto.Response getDispute(Long disputeId, CustomUserDetails userDetails) {
        SettlementDispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "이의신청을 찾을 수 없습니다."));
        requireDisputeReadAccess(dispute, userDetails);

        return convertToResponse(dispute);
    }

    /**
     * 이의신청 목록 조회 (관리자용)
     */
    @Transactional(readOnly = true)
    public Page<SettlementDisputeDto.ListResponse> getDisputeList(Pageable pageable, CustomUserDetails userDetails) {
        requireAdmin(userDetails);
        return disputeRepository.findAllWithSettlement(pageable)
                .map(this::convertToListResponse);
    }

    /**
     * 관리자 이의신청 검토/처리
     */
    @Transactional
    public SettlementDisputeDto.Response reviewDispute(SettlementDisputeDto.AdminReviewRequest request,
                                                       CustomUserDetails userDetails) {
        SettlementDispute dispute = disputeRepository.findById(request.getDisputeId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "이의신청을 찾을 수 없습니다."));
        requireAdmin(userDetails);

        // 상태 업데이트
        dispute.setStatus(request.getStatus());
        dispute.setAdminResponse(request.getAdminResponse());
        dispute.setReviewedAt(LocalDateTime.now());
        dispute.setAdminId(userDetails.getUserId());
        dispute.setAdminName(resolvePrincipalName(userDetails));

        // Settlement의 dispute 상태도 업데이트
        Settlement settlement = dispute.getSettlement();
        if (request.getStatus() == SettlementDispute.DisputeProcessStatus.RESOLVED ||
                request.getStatus() == SettlementDispute.DisputeProcessStatus.REJECTED) {
            settlement.setDisputeStatus(DisputeStatus.RESOLVED);
        } else if (request.getStatus() == SettlementDispute.DisputeProcessStatus.RAISED) {
            settlement.setDisputeStatus(DisputeStatus.RAISED);
        }

        disputeRepository.save(dispute);
        settlementRepository.save(settlement);

        log.info("이의신청 검토 완료 - DisputeId: {}, Status: {}, AdminId: {}",
                dispute.getDisputeId(), request.getStatus(), userDetails.getUserId());

        return convertToResponse(dispute);
    }

    /**
     * 이의신청 파일 다운로드 URL 생성
     */
    @Transactional(readOnly = true)
    public String getFileDownloadUrl(Long fileId) {
        SettlementDisputeFile file = disputeFileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."));

        return "/api/settlements/disputes/files/download?fileId=" + fileId;
    }

    @Transactional(readOnly = true)
    public String getAuthorizedFileKey(Long fileId, CustomUserDetails userDetails) {
        SettlementDisputeFile file = disputeFileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."));
        requireDisputeReadAccess(file.getDispute(), userDetails);
        return file.getS3Key();
    }

    /**
     * 엔티티를 응답 DTO로 변환
     */
    private SettlementDisputeDto.Response convertToResponse(SettlementDispute dispute) {
        List<SettlementDisputeDto.Response.FileInfo> fileInfos = dispute.getDisputeFiles().stream()
                .map(file -> SettlementDisputeDto.Response.FileInfo.builder()
                        .fileId(file.getFileId())
                        .originalFilename(file.getOriginalFilename())
                        .downloadUrl(getFileDownloadUrl(file.getFileId()))
                        .fileSize(file.getFileSize())
                        .fileType(file.getFileType())
                        .uploadOrder(file.getUploadOrder())
                        .createdAt(file.getCreatedAt())
                        .build())
                .toList();

        return SettlementDisputeDto.Response.builder()
                .disputeId(dispute.getDisputeId())
                .settlementId(dispute.getSettlement().getSettlementId())
                .eventTitle(dispute.getSettlement().getEventTitle())
                .requesterName(dispute.getRequesterName())
                .disputeReason(dispute.getDisputeReason())
                .files(fileInfos)
                .status(dispute.getStatus())
                .adminResponse(dispute.getAdminResponse())
                .adminName(dispute.getAdminName())
                .submittedAt(dispute.getSubmittedAt())
                .reviewedAt(dispute.getReviewedAt())
                .build();
    }

    /**
     * 특정 정산의 이의신청 조회
     */
    @Transactional(readOnly = true)
    public Optional<SettlementDisputeDto.Response> getDisputeBySettlementId(Long settlementId,
                                                                            CustomUserDetails userDetails) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "정산 정보를 찾을 수 없습니다."));
        requireSettlementReadAccess(settlement, userDetails);

        return disputeRepository.findBySettlement_SettlementId(settlementId)
                .map(dispute -> {
                    requireDisputeReadAccess(dispute, userDetails);
                    return convertToResponse(dispute);
                });
    }

    /**
     * 이의신청 통계 조회
     */
    public SettlementDisputeController.DisputeStatistics getDisputeStatistics() {
        long submitted = disputeRepository.countByStatus(SettlementDispute.DisputeProcessStatus.RAISED);
        long underReview = disputeRepository.countByStatus(SettlementDispute.DisputeProcessStatus.UNDER_REVIEW);
        long resolved = disputeRepository.countByStatus(SettlementDispute.DisputeProcessStatus.RESOLVED);
        long rejected = disputeRepository.countByStatus(SettlementDispute.DisputeProcessStatus.REJECTED);

        return new SettlementDisputeController.DisputeStatistics(submitted, underReview, resolved, rejected);
    }

    /**
     * 엔티티를 목록 응답 DTO로 변환
     */
    private SettlementDisputeDto.ListResponse convertToListResponse(SettlementDispute dispute) {
        return SettlementDisputeDto.ListResponse.builder()
                .disputeId(dispute.getDisputeId())
                .settlementId(dispute.getSettlement().getSettlementId())
                .eventTitle(dispute.getSettlement().getEventTitle())
                .requesterName(dispute.getRequesterName())
                .status(dispute.getStatus())
                .fileCount(dispute.getDisputeFiles().size())
                .submittedAt(dispute.getSubmittedAt())
                .build();
    }

    private void requireAuthenticated(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUserId() == null) {
            throw new AccessDeniedException("로그인이 필요합니다.");
        }
    }

    private void requireAdmin(CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        if (!ROLE_ADMIN.equals(userDetails.getRoleCode())) {
            throw new AccessDeniedException("정산 이의신청 관리자 권한이 없습니다.");
        }
    }

    private void requireEventManager(CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        if (!ROLE_EVENT_MANAGER.equals(userDetails.getRoleCode())) {
            throw new AccessDeniedException("정산 이의신청 권한이 없습니다.");
        }
    }

    private void requireManagedSettlementAccess(Settlement settlement, CustomUserDetails userDetails) {
        requireEventManager(userDetails);
        if (!isManagedBy(settlement.getEvent(), userDetails.getUserId())) {
            throw new AccessDeniedException("관리 중인 정산 이의신청만 처리할 수 있습니다.");
        }
    }

    private void requireSettlementReadAccess(Settlement settlement, CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        if (ROLE_ADMIN.equals(userDetails.getRoleCode())) {
            return;
        }
        if (ROLE_EVENT_MANAGER.equals(userDetails.getRoleCode())
                && isManagedBy(settlement.getEvent(), userDetails.getUserId())) {
            return;
        }
        throw new AccessDeniedException("정산 이의신청을 조회할 권한이 없습니다.");
    }

    private void requireDisputeReadAccess(SettlementDispute dispute, CustomUserDetails userDetails) {
        requireAuthenticated(userDetails);
        if (ROLE_ADMIN.equals(userDetails.getRoleCode())) {
            return;
        }
        if (ROLE_EVENT_MANAGER.equals(userDetails.getRoleCode())
                && isManagedBy(dispute.getSettlement().getEvent(), userDetails.getUserId())) {
            return;
        }
        throw new AccessDeniedException("정산 이의신청을 조회할 권한이 없습니다.");
    }

    private boolean isManagedBy(Event event, Long userId) {
        return event != null
                && event.getManager() != null
                && event.getManager().getUserId() != null
                && event.getManager().getUserId().equals(userId);
    }

    private String resolvePrincipalName(CustomUserDetails userDetails) {
        String principalName = userDetails.getName();
        if (principalName != null && !principalName.isBlank()) {
            return principalName;
        }
        return userRepository.findById(userDetails.getUserId())
                .map(user -> user.getName())
                .filter(name -> name != null && !name.isBlank())
                .orElse(null);
    }
}
