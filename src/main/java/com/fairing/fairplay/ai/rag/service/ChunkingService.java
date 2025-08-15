package com.fairing.fairplay.ai.rag.service;

import com.fairing.fairplay.ai.rag.domain.Chunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 청킹 전략 서비스 (600-800자 권장)
 */
@Service
public class ChunkingService {

    private static final int MIN_CHUNK_SIZE = 50;   // 50자로 대폭 축소
    private static final int MAX_CHUNK_SIZE = 400;  // 400자로 축소
    private static final int PREFERRED_CHUNK_SIZE = 300; // 300자로 축소
    
    // 문장 경계 정규식 (한국어 고려)
    private static final Pattern SENTENCE_PATTERN = Pattern.compile(
        "[.!?]+\\s+|[。！？]+\\s*|[\\n]{2,}"
    );
    
    // 문단 분리 정규식
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("\\n\\s*\\n");

    public List<Chunk> chunkDocument(String docId, String content) {
        List<Chunk> chunks = new ArrayList<>();
        
        if (content == null || content.trim().isEmpty()) {
            return chunks;
        }
        
        String cleanContent = preprocessText(content);
        List<String> chunkTexts = performChunking(cleanContent);
        
        String now = String.valueOf(System.currentTimeMillis());
        
        for (String chunkText : chunkTexts) {
            if (chunkText.trim().length() < MIN_CHUNK_SIZE) {
                continue; // 너무 작은 청크는 제외
            }
            
            Chunk chunk = Chunk.builder()
                .chunkId(generateChunkId())
                .docId(docId)
                .text(chunkText.trim())
                .createdAt(now)
                .build();
            
            chunks.add(chunk);
        }
        
        return chunks;
    }
    
    private String preprocessText(String text) {
        return text
            .replaceAll("\\s+", " ")  // 여러 공백을 하나로
            .replaceAll("\\n\\s*\\n", "\n\n")  // 문단 구분 정규화
            .trim();
    }
    
    private List<String> performChunking(String text) {
        List<String> chunks = new ArrayList<>();
        
        // 1. 문단 우선 분할
        String[] paragraphs = PARAGRAPH_PATTERN.split(text);
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;
            
            // 현재 청크에 문단을 추가했을 때 크기 확인
            String testChunk = currentChunk.toString();
            if (!testChunk.isEmpty()) {
                testChunk += "\n\n" + paragraph;
            } else {
                testChunk = paragraph;
            }
            
            if (testChunk.length() <= MAX_CHUNK_SIZE) {
                // 추가 가능
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            } else {
                // 현재 청크를 저장하고 새 청크 시작
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk.setLength(0);
                }
                
                // 문단이 너무 크면 문장 단위로 분할
                if (paragraph.length() > MAX_CHUNK_SIZE) {
                    chunks.addAll(splitLargeParagraph(paragraph));
                } else {
                    currentChunk.append(paragraph);
                }
            }
        }
        
        // 마지막 청크 추가
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }
    
    private List<String> splitLargeParagraph(String paragraph) {
        List<String> chunks = new ArrayList<>();
        
        String[] sentences = SENTENCE_PATTERN.split(paragraph);
        StringBuilder currentChunk = new StringBuilder();
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;
            
            String testChunk = currentChunk.toString();
            if (!testChunk.isEmpty()) {
                testChunk += " " + sentence;
            } else {
                testChunk = sentence;
            }
            
            if (testChunk.length() <= MAX_CHUNK_SIZE) {
                if (currentChunk.length() > 0) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
            } else {
                // 현재 청크 저장
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk.setLength(0);
                }
                
                // 문장이 너무 크면 강제 분할
                if (sentence.length() > MAX_CHUNK_SIZE) {
                    chunks.addAll(forceChunk(sentence));
                } else {
                    currentChunk.append(sentence);
                }
            }
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }
    
    private List<String> forceChunk(String text) {
        List<String> chunks = new ArrayList<>();
        
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_CHUNK_SIZE, text.length());
            
            // 단어 경계에서 자르기 시도
            if (end < text.length()) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > start + MIN_CHUNK_SIZE) {
                    end = lastSpace;
                }
            }
            
            chunks.add(text.substring(start, end).trim());
            start = end;
        }
        
        return chunks;
    }
    
    private String generateChunkId() {
        return "chunk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}