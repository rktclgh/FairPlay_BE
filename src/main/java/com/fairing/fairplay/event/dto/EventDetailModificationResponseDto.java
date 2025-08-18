package com.fairing.fairplay.event.dto;

import com.fairing.fairplay.event.entity.EventDetailModificationRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventDetailModificationResponseDto {
    
    private Long requestId;
    private Long eventId;
    private String eventTitle;
    private Long requestedBy;
    private String eventCode;
    private String statusCode;
    private String statusName;
    private Long processedBy;
    private LocalDateTime processedAt;
    private String adminComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private EventDetailModificationDto originalData;
    private EventDetailModificationDto modifiedData;

    public static EventDetailModificationResponseDto from(EventDetailModificationRequest request) {
        EventDetailModificationResponseDto dto = new EventDetailModificationResponseDto();
        dto.setRequestId(request.getRequestId());
        dto.setEventId(request.getEvent().getEventId());
        dto.setEventTitle(request.getEvent().getTitleKr());
        dto.setRequestedBy(request.getRequestedBy());
        dto.setEventCode(request.getEvent().getEventCode());
        dto.setStatusCode(request.getStatus().getCode());
        dto.setStatusName(request.getStatus().getName());
        dto.setProcessedBy(request.getProcessedBy());
        dto.setProcessedAt(request.getProcessedAt());
        dto.setAdminComment(request.getAdminComment());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setUpdatedAt(request.getUpdatedAt());
        dto.setOriginalData(request.getOriginalDataAsDto());
        dto.setModifiedData(request.getModifiedDataAsDto());
        return dto;
    }
}