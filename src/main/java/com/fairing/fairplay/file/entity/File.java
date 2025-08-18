package com.fairing.fairplay.file.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "file")
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id", nullable = false)
    private Long id;

    @Column(name = "file_url", length = 512, nullable = false)
    private String fileUrl;

    @Column(nullable = false)
    private boolean referenced;

    @Column(name = "file_type", length = 50, nullable = false)
    private String fileType;

    @Column(length = 100)
    private String directory;

    @Column(name = "original_file_name", length = 100, nullable = false)
    private String originalFileName;

    @Column(name = "stored_file_name", length = 255, nullable = false)
    private String storedFileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private boolean thumbnail = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

}
