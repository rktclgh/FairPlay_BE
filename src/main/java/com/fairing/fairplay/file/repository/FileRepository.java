package com.fairing.fairplay.file.repository;

import com.fairing.fairplay.file.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {
    Optional<File> findByFileUrlContaining(String s3Key);
    
    @Query("SELECT f FROM File f JOIN FileLink fl ON f.id = fl.file.id WHERE fl.targetType = :targetType AND fl.targetId = :targetId")
    List<File> findByTargetTypeAndTargetId(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    @Query("SELECT f, fl.targetId FROM File f JOIN FileLink fl ON f.id = fl.file.id WHERE fl.targetType = :targetType AND fl.targetId IN :targetIds")
    List<Object[]> findByTargetTypeAndTargetIdIn(@Param("targetType") String targetType, @Param("targetIds") List<Long> targetIds);
}
