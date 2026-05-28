package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.ai.rag.domain.Chunk;
import com.fairing.fairplay.ai.rag.repository.RagChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RagChunkWriteService {

    private final RagChunkRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void replaceDocument(String docId, List<Chunk> chunks) {
        repository.deleteDocument(docId);
        if (chunks != null && !chunks.isEmpty()) {
            repository.saveChunks(chunks);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteDocument(String docId) {
        repository.deleteDocument(docId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearAllDocuments() {
        repository.clearAllData();
    }
}
