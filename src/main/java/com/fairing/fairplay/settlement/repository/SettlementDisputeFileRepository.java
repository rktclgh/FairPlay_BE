package com.fairing.fairplay.settlement.repository;

import com.fairing.fairplay.settlement.entity.SettlementDisputeFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SettlementDisputeFileRepository extends JpaRepository<SettlementDisputeFile, Long> {
    /**
     * 특정 이의신청의 파일 목록 조회 (업로드 순서대로)
     */
    List<SettlementDisputeFile> findByDispute_DisputeIdOrderByUploadOrderAsc(Long disputeId);

    /**
     * 특정 이의신청의 파일 개수 조회
     */
    long countByDispute_DisputeId(Long disputeId);

    /**
     * S3 키로 파일 조회
     */
    List<SettlementDisputeFile> findByS3Key(String s3Key);

    /**
     * 특정 이의신청의 특정 타입 파일들만 조회
     */
    @Query("SELECT f FROM SettlementDisputeFile f " +
            "WHERE f.dispute.disputeId = :disputeId AND f.fileType = :fileType " +
            "ORDER BY f.uploadOrder ASC")
    List<SettlementDisputeFile> findByDisputeIdAndFileType(
            @Param("disputeId") Long disputeId,
            @Param("fileType") SettlementDisputeFile.DisputeFileType fileType);
}
