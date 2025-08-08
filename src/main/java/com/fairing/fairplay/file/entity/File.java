package com.fairing.fairplay.file.entity;

import com.fairing.fairplay.event.entity.Event;
import com.fairing.fairplay.event.entity.EventApply;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "file")
public class File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_apply_id")
    private EventApply eventApply;

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

    @Builder
    public File(Event event, EventApply eventApply, String fileUrl, boolean referenced, String fileType, String directory, String originalFileName, String storedFileName, Long fileSize, boolean thumbnail) {
        this.event = event;
        this.eventApply = eventApply;
        this.fileUrl = fileUrl;
        this.referenced = referenced;
        this.fileType = fileType;
        this.directory = directory;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.fileSize = fileSize;
        this.thumbnail = thumbnail;
    }
}
