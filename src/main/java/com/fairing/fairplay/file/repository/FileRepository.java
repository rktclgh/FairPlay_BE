package com.fairing.fairplay.file.repository;

import com.fairing.fairplay.file.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {
}
