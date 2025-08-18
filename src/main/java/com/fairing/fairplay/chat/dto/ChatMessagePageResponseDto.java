package com.fairing.fairplay.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePageResponseDto {
    private List<ChatMessageResponseDto> messages;
    private Long nextCursor; // 다음 페이지를 위한 커서 (마지막 메시지 ID)
    private Boolean hasNext; // 다음 페이지 존재 여부
    private Integer currentPage;
    private Integer pageSize;
    private Long totalElements;
}