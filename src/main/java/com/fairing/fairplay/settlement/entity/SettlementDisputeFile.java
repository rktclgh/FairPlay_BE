package com.fairing.fairplay.settlement.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_dispute_file")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDisputeFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", nullable = false)
    private SettlementDispute dispute;

    @Column(name = "s3_key", nullable = false, length = 500)
    private String s3Key; // S3 저장 경로

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename; // 원본 파일명

    @Column(name = "file_size", nullable = false)
    private Long fileSize; // 파일 크기 (bytes)

    @Column(name = "content_type", length = 100)
    private String contentType; // MIME 타입

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", length = 20, nullable = false)
    private DisputeFileType fileType; // PDF, EXCEL

    @Column(name = "upload_order", nullable = false)
    private Integer uploadOrder; // 업로드 순서

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // 이의신청 파일 타입
    public enum DisputeFileType {
        PDF("PDF 문서"),
        EXCEL("엑셀 파일");

        private final String description;

        DisputeFileType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public static DisputeFileType fromContentType(String contentType) {
            if (contentType == null) return null;

            if (contentType.equals("application/pdf")) {
                return PDF;
            } else if (contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                    contentType.equals("application/vnd.ms-excel")) {
                return EXCEL;
            }
            return null;
        }
    }
}