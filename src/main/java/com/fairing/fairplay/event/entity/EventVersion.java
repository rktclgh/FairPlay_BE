package com.fairing.fairplay.event.entity;

import com.fairing.fairplay.core.util.JsonUtil;
import com.fairing.fairplay.event.dto.EventSnapshotDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_version", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"event_id", "version_number"})
})
public class EventVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_version_id")
    private Long eventVersionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(columnDefinition = "JSON")
    private String snapshot;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public EventSnapshotDto getSnapshotAsDto() {
        return JsonUtil.fromJson(this.snapshot, EventSnapshotDto.class);
    }

    public void setSnapshotFromDto(EventSnapshotDto dto) {
        this.snapshot = JsonUtil.toJson(dto);
    }
}