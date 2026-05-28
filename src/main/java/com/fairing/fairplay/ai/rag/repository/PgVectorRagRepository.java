package com.fairing.fairplay.ai.rag.repository;

import com.fairing.fairplay.ai.rag.domain.Chunk;
import com.fairing.fairplay.ai.rag.domain.SearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

@Repository
@RequiredArgsConstructor
public class PgVectorRagRepository implements RagChunkRepository {

    private static final int KEYWORD_SCAN_LIMIT_MULTIPLIER = 8;
    private static final RowMapper<Chunk> CHUNK_ROW_MAPPER = (rs, rowNum) -> Chunk.builder()
        .chunkId(rs.getString("chunk_id"))
        .docId(rs.getString("doc_id"))
        .text(rs.getString("text"))
        .docType(rs.getString("doc_type"))
        .visibility(rs.getString("visibility"))
        .ownerUserId(rs.getObject("owner_user_id", Long.class))
        .eventId(rs.getObject("event_id", Long.class))
        .boothId(rs.getObject("booth_id", Long.class))
        .reservationId(rs.getObject("reservation_id", Long.class))
        .createdAt(rs.getString("created_at"))
        .build();
    private static final RowMapper<SearchResult.ScoredChunk> SCORED_CHUNK_ROW_MAPPER = (rs, rowNum) -> SearchResult.ScoredChunk.builder()
        .chunk(CHUNK_ROW_MAPPER.mapRow(rs, rowNum))
        .similarity(rs.getDouble("similarity"))
        .build();

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void saveChunks(List<Chunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO rag_chunks (
                chunk_id, doc_id, text, embedding, doc_type, visibility,
                owner_user_id, event_id, booth_id, reservation_id, created_at
            )
            VALUES (?, ?, ?, CAST(? AS vector), ?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (chunk_id) DO UPDATE SET
                doc_id = EXCLUDED.doc_id,
                text = EXCLUDED.text,
                embedding = EXCLUDED.embedding,
                doc_type = EXCLUDED.doc_type,
                visibility = EXCLUDED.visibility,
                owner_user_id = EXCLUDED.owner_user_id,
                event_id = EXCLUDED.event_id,
                booth_id = EXCLUDED.booth_id,
                reservation_id = EXCLUDED.reservation_id,
                created_at = EXCLUDED.created_at,
                updated_at = now()
            """;

        jdbcTemplate.batchUpdate(sql, chunks, chunks.size(), (ps, chunk) -> {
            ps.setString(1, chunk.getChunkId());
            ps.setString(2, chunk.getDocId());
            ps.setString(3, chunk.getText());
            ps.setString(4, toVectorLiteral(chunk.getEmbedding()));
            ps.setString(5, chunk.getDocType());
            ps.setString(6, chunk.getVisibility());
            ps.setObject(7, chunk.getOwnerUserId());
            ps.setObject(8, chunk.getEventId());
            ps.setObject(9, chunk.getBoothId());
            ps.setObject(10, chunk.getReservationId());
        });
    }

    @Override
    public List<SearchResult.ScoredChunk> searchSimilar(float[] queryEmbedding, int topK, double threshold) {
        return searchSimilarByScope(queryEmbedding, topK, threshold, "");
    }

    @Override
    public List<SearchResult.ScoredChunk> searchPublicSimilar(float[] queryEmbedding, int topK, double threshold) {
        return searchSimilarByScope(queryEmbedding, topK, threshold, "AND visibility = 'PUBLIC'");
    }

    @Override
    public List<SearchResult.ScoredChunk> searchPublicSimilarByTypes(
        List<String> docTypes,
        float[] queryEmbedding,
        int topK,
        double threshold
    ) {
        return searchSimilarByScope(queryEmbedding, topK, threshold, publicTypeScope(docTypes), docTypes.toArray());
    }

    @Override
    public List<SearchResult.ScoredChunk> searchUserSimilar(Long userId, float[] queryEmbedding, int topK, double threshold) {
        return searchSimilarByScope(queryEmbedding, topK, threshold, "AND owner_user_id = ?", userId);
    }

    @Override
    public List<SearchResult.ScoredChunk> searchUserSimilarByTypes(
        Long userId,
        List<String> docTypes,
        float[] queryEmbedding,
        int topK,
        double threshold
    ) {
        Object[] params = new Object[docTypes.size() + 1];
        params[0] = userId;
        for (int i = 0; i < docTypes.size(); i++) {
            params[i + 1] = docTypes.get(i);
        }
        return searchSimilarByScope(queryEmbedding, topK, threshold, userTypeScope(docTypes), params);
    }

    @Override
    public List<SearchResult.ScoredChunk> searchKeyword(String query, int topK) {
        return searchKeywordByScope(query, topK, "");
    }

    @Override
    public List<SearchResult.ScoredChunk> searchPublicKeyword(String query, int topK) {
        return searchKeywordByScope(query, topK, "AND visibility = 'PUBLIC'");
    }

    @Override
    public List<SearchResult.ScoredChunk> searchPublicKeywordByTypes(List<String> docTypes, String query, int topK) {
        return searchKeywordByScope(query, topK, publicTypeScope(docTypes), docTypes.toArray());
    }

    @Override
    public List<SearchResult.ScoredChunk> searchUserKeyword(Long userId, String query, int topK) {
        return searchKeywordByScope(query, topK, "AND owner_user_id = ?", userId);
    }

    @Override
    public List<SearchResult.ScoredChunk> searchUserKeywordByTypes(Long userId, List<String> docTypes, String query, int topK) {
        Object[] params = new Object[docTypes.size() + 1];
        params[0] = userId;
        for (int i = 0; i < docTypes.size(); i++) {
            params[i + 1] = docTypes.get(i);
        }
        return searchKeywordByScope(query, topK, userTypeScope(docTypes), params);
    }

    @Override
    public List<Chunk> findByDocId(String docId) {
        return jdbcTemplate.query("""
            SELECT chunk_id, doc_id, text, doc_type, visibility, owner_user_id, event_id, booth_id, reservation_id,
                   created_at::text AS created_at
            FROM rag_chunks
            WHERE doc_id = ?
            ORDER BY chunk_id
            """, chunkRowMapper(), docId);
    }

    @Override
    public void deleteDocument(String docId) {
        jdbcTemplate.update("DELETE FROM rag_chunks WHERE doc_id = ?", docId);
    }

    @Override
    public void clearAllData() {
        jdbcTemplate.update("TRUNCATE TABLE rag_chunks");
    }

    @Override
    public long getTotalChunkCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rag_chunks", Long.class);
        return count != null ? count : 0L;
    }

    private List<SearchResult.ScoredChunk> searchSimilarByScope(
        float[] queryEmbedding,
        int topK,
        double threshold,
        String scopeClause,
        Object... scopeParams
    ) {
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            return List.of();
        }

        String sql = """
            SELECT chunk_id, doc_id, text, doc_type, visibility, owner_user_id, event_id, booth_id, reservation_id,
                   created_at::text AS created_at,
                   1 - (embedding <=> CAST(? AS vector)) AS similarity
            FROM rag_chunks
            WHERE embedding IS NOT NULL
              AND 1 - (embedding <=> CAST(? AS vector)) >= ?
            """ + scopeClause + """

            ORDER BY embedding <=> CAST(? AS vector)
            LIMIT ?
            """;

        String vector = toVectorLiteral(queryEmbedding);
        return jdbcTemplate.query(sql, ps -> {
            int index = 1;
            ps.setString(index++, vector);
            ps.setString(index++, vector);
            ps.setDouble(index++, threshold);
            for (Object scopeParam : scopeParams) {
                ps.setObject(index++, scopeParam);
            }
            ps.setString(index++, vector);
            ps.setInt(index, Math.max(topK, 1));
        }, scoredChunkRowMapper());
    }

    private List<SearchResult.ScoredChunk> searchKeywordByScope(
        String query,
        int topK,
        String scopeClause,
        Object... scopeParams
    ) {
        List<String> keywords = keywords(query);
        if (keywords.isEmpty()) {
            return List.of();
        }

        String condition = keywordCondition(keywords.size());
        String sql = """
            SELECT chunk_id, doc_id, text, doc_type, visibility, owner_user_id, event_id, booth_id, reservation_id,
                   created_at::text AS created_at
            FROM rag_chunks
            WHERE (
            """ + condition + """
            )
            """ + scopeClause + """

            ORDER BY updated_at DESC
            LIMIT ?
            """;

        int scanLimit = Math.max(topK, 1) * KEYWORD_SCAN_LIMIT_MULTIPLIER;
        List<Chunk> matches = jdbcTemplate.query(sql, ps -> {
            int index = 1;
            for (String keyword : keywords) {
                ps.setString(index++, "%" + keyword.toLowerCase(Locale.ROOT) + "%");
            }
            for (Object scopeParam : scopeParams) {
                ps.setObject(index++, scopeParam);
            }
            ps.setInt(index, scanLimit);
        }, chunkRowMapper());

        return matches.stream()
            .map(chunk -> SearchResult.ScoredChunk.builder()
                .chunk(chunk)
                .similarity(scoreKeyword(chunk.getText(), query, keywords))
                .build())
            .filter(scored -> scored.getSimilarity() > 0)
            .sorted((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()))
            .limit(topK)
            .toList();
    }

    private String keywordCondition(int keywordCount) {
        StringJoiner joiner = new StringJoiner(" OR ");
        for (int i = 0; i < keywordCount; i++) {
            joiner.add("lower(text) LIKE ?");
        }
        return joiner.toString();
    }

    private String publicTypeScope(List<String> docTypes) {
        return "AND visibility = 'PUBLIC' AND doc_type IN (" + placeholders(docTypes) + ")";
    }

    private String userTypeScope(List<String> docTypes) {
        return "AND owner_user_id = ? AND doc_type IN (" + placeholders(docTypes) + ")";
    }

    private String placeholders(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("docTypes must not be empty");
        }
        return String.join(",", java.util.Collections.nCopies(values.size(), "?"));
    }

    private List<String> keywords(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        List<String> keywords = new ArrayList<>();
        for (String keyword : query.trim().split("\\s+")) {
            if (!keyword.isBlank()) {
                keywords.add(keyword);
            }
        }
        return keywords;
    }

    private double scoreKeyword(String text, String query, List<String> keywords) {
        String lowerText = text.toLowerCase(Locale.ROOT);
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        double score = lowerText.contains(normalizedQuery) ? 50.0 : 0.0;

        for (String keyword : keywords) {
            String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
            if (lowerText.contains(lowerKeyword)) {
                score += 10.0;
                if (isInImportantField(lowerText, lowerKeyword)) {
                    score += 20.0;
                }
            }
        }

        return Math.min(score / Math.max(keywords.size(), 1), 100.0);
    }

    private boolean isInImportantField(String lowerText, String lowerKeyword) {
        for (String line : lowerText.split("\\n")) {
            if ((line.contains("제목") || line.contains("이벤트명") || line.contains("검색 키워드"))
                && line.contains(lowerKeyword)) {
                return true;
            }
        }
        return false;
    }

    private String toVectorLiteral(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException("RAG embedding vector must not be empty");
        }

        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (float value : vector) {
            joiner.add(Float.toString(value));
        }
        return joiner.toString();
    }

    private RowMapper<Chunk> chunkRowMapper() {
        return CHUNK_ROW_MAPPER;
    }

    private RowMapper<SearchResult.ScoredChunk> scoredChunkRowMapper() {
        return SCORED_CHUNK_ROW_MAPPER;
    }
}
