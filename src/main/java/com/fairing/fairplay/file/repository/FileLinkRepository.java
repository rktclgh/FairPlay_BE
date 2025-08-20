package com.fairing.fairplay.file.repository;

import com.fairing.fairplay.file.entity.FileLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FileLinkRepository extends JpaRepository<FileLink, Long> {
    
    List<FileLink> findByTargetTypeAndTargetId(String targetType, Long targetId);
    
    List<FileLink> findByFileId(Long fileId);
    
    @Query("SELECT fl FROM FileLink fl WHERE fl.targetType = :targetType AND fl.targetId = :targetId")
    List<FileLink> findFilesByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);
    
    void deleteByTargetTypeAndTargetId(String targetType, Long targetId);
}