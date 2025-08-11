package com.fairing.fairplay.event.dto;

import com.fairing.fairplay.event.entity.EventVersion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventVersionResponseDto {
    
    private Long eventVersionId;
    private Long eventId;
    private Integer versionNumber;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    private EventSnapshotDto snapshot;

    public static EventVersionResponseDto from(EventVersion eventVersion) {
        return EventVersionResponseDto.builder()
                .eventVersionId(eventVersion.getEventVersionId())
                .eventId(eventVersion.getEvent().getEventId())
                .versionNumber(eventVersion.getVersionNumber())
                .updatedBy(eventVersion.getUpdatedBy())
                .updatedAt(eventVersion.getUpdatedAt())
                .snapshot(eventVersion.getSnapshotAsDto())
                .build();
    }
}