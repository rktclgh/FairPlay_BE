package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.ai.rag.domain.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkingServiceTest {

    private final ChunkingService chunkingService = new ChunkingService();

    @Test
    void chunkingPreservesDocumentSectionsInsteadOfFlatteningAllWhitespace() {
        Document document = Document.builder()
            .docId("reservation_141")
            .docType("USER_RESERVATION")
            .visibility("USER_PRIVATE")
            .ownerUserId(1081L)
            .content("""
                === 개인 예약 내역 ===
                행사명: 2025 트렌드페어

                === 예약 티켓 ===
                티켓명: 멋쟁이 티켓
                예약 상태: 완료
                총 결제 금액: 200,000원
                예약일: 2025-08-21T13:00
                문의처: fairplay@example.com
                관람일: 2025-08-22
                관람 시간: 10:00 ~ 18:00
                """)
            .build();

        assertThat(chunkingService.chunkDocument(document))
            .singleElement()
            .satisfies(chunk -> assertThat(chunk.getText())
                .contains("=== 개인 예약 내역 ===\n")
                .contains("\n\n=== 예약 티켓 ==="));
    }
}
