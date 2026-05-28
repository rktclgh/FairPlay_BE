package com.fairing.fairplay.ai.rag.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 문서 도메인 객체
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    private String docId;
    private String title;
    private String content;
    private String category;
    private String docType;
    private String visibility;
    private Long ownerUserId;
    private Long eventId;
    private Long boothId;
    private Long reservationId;
    private long createdAt;
    private long updatedAt;
}
