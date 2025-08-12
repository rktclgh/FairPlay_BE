package com.fairing.fairplay.file.repository;

import com.fairing.fairplay.file.entity.File;
import com.fairing.fairplay.event.entity.EventApply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {
    Optional<File> findByFileUrlContaining(String s3Key);
    List<File> findByEventApply(EventApply eventApply);
}
