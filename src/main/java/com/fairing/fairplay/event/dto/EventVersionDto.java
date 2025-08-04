package com.fairing.fairplay.event.dto;

import com.fairing.fairplay.event.entity.EventVersion;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EventVersionDto {
    private Long eventVersionId;
    private Integer versionNumber;
    private EventSnapshotDto snapshot;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    public static EventVersionDto from(EventVersion version) {
        return EventVersionDto.builder()
                .eventVersionId(version.getEventVersionId())
                .versionNumber(version.getVersionNumber())
                .snapshot(version.getSnapshotAsDto())
                .updatedBy(version.getUpdatedBy())
                .updatedAt(version.getUpdatedAt())
                .build();
    }
}
