package com.fairing.fairplay.settlement.dto;

import com.fairing.fairplay.settlement.entity.SettlementDispute;
import com.fairing.fairplay.settlement.entity.SettlementDisputeFile;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class SettlementDisputeDto {

    @Getter
    @Setter
    @Builder
    public static class CreateRequest {
        private Long settlementId;
        private String disputeReason;
        private List<String> tempFileKeys; // 임시 업로드된 파일들의 S3 키
    }

    @Getter
    @Setter
    @Builder
    public static class Response {
        private Long disputeId;
        private Long settlementId;
        private String eventTitle;
        private String requesterName;
        private String disputeReason;
        private List<FileInfo> files;
        private SettlementDispute.DisputeProcessStatus status;
        private String adminResponse;
        private String adminName;
        private LocalDateTime submittedAt;
        private LocalDateTime reviewedAt;

        @Getter
        @Setter
        @Builder
        public static class FileInfo {
            private Long fileId;
            private String originalFilename;
            private String downloadUrl;
            private Long fileSize;
            private SettlementDisputeFile.DisputeFileType fileType;
            private Integer uploadOrder;
            private LocalDateTime createdAt;
        }
    }

    @Getter
    @Setter
    @Builder
    public static class AdminReviewRequest {
        private Long disputeId;
        private SettlementDispute.DisputeProcessStatus status;
        private String adminResponse;
    }

    @Getter
    @Setter
    @Builder
    public static class ListResponse {
        private Long disputeId;
        private Long settlementId;
        private String eventTitle;
        private String requesterName;
        private SettlementDispute.DisputeProcessStatus status;
        private Integer fileCount;
        private LocalDateTime submittedAt;
    }
}