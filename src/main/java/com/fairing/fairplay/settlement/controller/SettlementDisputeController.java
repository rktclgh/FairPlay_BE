package com.fairing.fairplay.settlement.controller;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.service.AwsS3Service;
import com.fairing.fairplay.settlement.dto.SettlementDisputeDto;
import com.fairing.fairplay.settlement.entity.SettlementDisputeFile;
import com.fairing.fairplay.settlement.repository.SettlementDisputeFileRepository;
import com.fairing.fairplay.settlement.service.SettlementDisputeService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/settlements/disputes")
@RequiredArgsConstructor
@Slf4j
public class SettlementDisputeController {

    private final SettlementDisputeService disputeService;
    private final SettlementDisputeFileRepository disputeFileRepository;
    private final AwsS3Service awsS3Service;

    /**
     * 이의신청용 파일 임시 업로드
     */
    @PostMapping("/files/upload")
    @FunctionAuth("disputeUploadFiles")
    public ResponseEntity<SettlementDisputeDto.Response> disputeUploadFiles(
            @RequestParam("files") List<MultipartFile> files) {

        SettlementDisputeDto.Response response = disputeService.uploadDisputeFiles(files);
        return ResponseEntity.ok(response);
    }

    /**
     * 이의신청 제출
     */
    @PostMapping
    @FunctionAuth("submitDispute")
    public ResponseEntity<SettlementDisputeDto.Response> submitDispute(
            @RequestBody SettlementDisputeDto.CreateRequest request,
            @RequestHeader("User-Id") Long requesterId,
            @RequestHeader("User-Name") String requesterName) {

        SettlementDisputeDto.Response response = disputeService.submitDispute(request, requesterId, requesterName);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 이의신청 상세 조회
     */
    @GetMapping("/{disputeId}")
    @FunctionAuth("getDetailDispute")
    public ResponseEntity<SettlementDisputeDto.Response> getDetailDispute(@PathVariable Long disputeId) {
        SettlementDisputeDto.Response response = disputeService.getDispute(disputeId);
        return ResponseEntity.ok(response);
    }

    /**
     * 이의신청 목록 조회 (관리자용)
     */
    @GetMapping
    @FunctionAuth("getDisputeList")
    public ResponseEntity<Page<SettlementDisputeDto.ListResponse>> getDisputeList(Pageable pageable) {
        Page<SettlementDisputeDto.ListResponse> response = disputeService.getDisputeList(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 관리자 이의신청 검토/처리
     */
    @PutMapping("/review")
    @FunctionAuth("reviewDispute")
    public ResponseEntity<SettlementDisputeDto.Response> reviewDispute(
            @RequestBody SettlementDisputeDto.AdminReviewRequest request,
            @RequestHeader("Admin-Id") Long adminId,
            @RequestHeader("Admin-Name") String adminName) {

        SettlementDisputeDto.Response response = disputeService.reviewDispute(request, adminId);
        return ResponseEntity.ok(response);
    }

    /**
     * 이의신청 파일 다운로드
     */
    @GetMapping("/files/download")
    @FunctionAuth("downloadDisputeFile")
    public void downloadDisputeFile(@RequestParam Long fileId, HttpServletResponse response) throws IOException {
        SettlementDisputeFile file = disputeFileRepository.findById(fileId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."));

        // 파일 다운로드
        awsS3Service.downloadFile(file.getS3Key(), response);

        log.info("이의신청 파일 다운로드 - FileId: {}, S3Key: {}", fileId, file.getS3Key());
    }

    /**
     * 특정 정산의 이의신청 조회
     */
    @GetMapping("/settlement/{settlementId}")
    @FunctionAuth("getDisputeBySettlement")
    public ResponseEntity<SettlementDisputeDto.Response> getDisputeBySettlement(@PathVariable Long settlementId) {
        // 해당 정산의 이의신청이 있는지 조회
        return disputeService.getDisputeBySettlementId(settlementId)
                .map(dispute -> ResponseEntity.ok(dispute))
                .orElse(ResponseEntity.notFound().build());
    }



    // 통계 응답 DTO
    public static class DisputeStatistics {
        public long submitted;
        public long underReview;
        public long resolved;
        public long rejected;
        public long total;

        public DisputeStatistics(long submitted, long underReview, long resolved, long rejected) {
            this.submitted = submitted;
            this.underReview = underReview;
            this.resolved = resolved;
            this.rejected = rejected;
            this.total = submitted + underReview + resolved + rejected;
        }
    }
}