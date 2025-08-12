package com.fairing.fairplay.event.entity;

import com.fairing.fairplay.core.util.JsonUtil;
import com.fairing.fairplay.event.dto.EventDetailModificationDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "event_detail_modification_request")
public class EventDetailModificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "requested_by", nullable = false)
    private Long requestedBy;

    @Column(name = "original_data", columnDefinition = "JSON", nullable = false)
    private String originalData;

    @Column(name = "modified_data", columnDefinition = "JSON", nullable = false)
    private String modifiedData;

    @Column(name = "new_file_keys", columnDefinition = "TEXT")
    private String newFileKeysJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private UpdateStatusCode status;

    @Column(name = "processed_by")
    private Long processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "admin_comment", columnDefinition = "TEXT")
    private String adminComment;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public EventDetailModificationDto getOriginalDataAsDto() {
        return JsonUtil.fromJson(this.originalData, EventDetailModificationDto.class);
    }

    public void setOriginalDataFromDto(EventDetailModificationDto dto) {
        this.originalData = JsonUtil.toJson(dto);
    }

    public EventDetailModificationDto getModifiedDataAsDto() {
        return JsonUtil.fromJson(this.modifiedData, EventDetailModificationDto.class);
    }

    public void setModifiedDataFromDto(EventDetailModificationDto dto) {
        this.modifiedData = JsonUtil.toJson(dto);
    }

    public void approve(Long approvedBy, String adminComment) {
        this.processedBy = approvedBy;
        this.processedAt = LocalDateTime.now();
        this.adminComment = adminComment;
    }

    public void reject(Long rejectedBy, String adminComment) {
        this.processedBy = rejectedBy;
        this.processedAt = LocalDateTime.now();
        this.adminComment = adminComment;
    }
}