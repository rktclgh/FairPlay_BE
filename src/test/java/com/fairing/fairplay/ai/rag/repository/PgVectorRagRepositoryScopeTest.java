package com.fairing.fairplay.ai.rag.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

class PgVectorRagRepositoryScopeTest {

    private JdbcTemplate jdbcTemplate;
    private PgVectorRagRepository repository;

    @BeforeEach
    void setUp() {
        jdbcTemplate = org.mockito.Mockito.mock(JdbcTemplate.class);
        repository = new PgVectorRagRepository(jdbcTemplate);
    }

    @Test
    void publicKeywordSearchUsesVisibilityScopeInsteadOfDocIdPrefixHeuristic() {
        stubQuery();

        repository.searchPublicKeyword("2025 명원 세계차 품평대회", 5);

        String sql = capturedSql();
        assertThat(sql).contains("AND visibility = 'PUBLIC'");
        assertThat(sql).doesNotContain("doc_id NOT LIKE");
    }

    @Test
    void publicVectorSearchUsesVisibilityScopeInsteadOfDocIdPrefixHeuristic() {
        stubQuery();

        repository.searchPublicSimilar(vector(), 5, 0.1);

        String sql = capturedSql();
        assertThat(sql).contains("AND visibility = 'PUBLIC'");
        assertThat(sql).doesNotContain("doc_id NOT LIKE");
    }

    @Test
    void publicTypedSearchUsesPublicVisibilityScope() {
        stubQuery();

        repository.searchPublicKeywordByTypes(List.of("PUBLIC_EVENT"), "AWS Summit", 5);

        String sql = capturedSql();
        assertThat(sql).contains("AND visibility = 'PUBLIC' AND doc_type IN (?)");
    }

    @Test
    void userKeywordSearchScopesToOwnerPrivateChunks() {
        stubQuery();

        repository.searchUserKeyword(10L, "내 예약", 5);

        String sql = capturedSql();
        assertThat(sql).contains("AND visibility = 'USER_PRIVATE' AND owner_user_id = ?");
        assertThat(sql).doesNotContain("doc_id = ?");
    }

    @Test
    void userVectorSearchScopesToOwnerPrivateChunks() {
        stubQuery();

        repository.searchUserSimilar(10L, vector(), 5, 0.05);

        String sql = capturedSql();
        assertThat(sql).contains("AND visibility = 'USER_PRIVATE' AND owner_user_id = ?");
        assertThat(sql).doesNotContain("doc_id = ?");
    }

    @Test
    void userTypedSearchScopesToOwnerPrivateChunks() {
        stubQuery();

        repository.searchUserKeywordByTypes(10L, List.of("USER_RESERVATION"), "내 예약", 5);

        String sql = capturedSql();
        assertThat(sql).contains("AND visibility = 'USER_PRIVATE' AND owner_user_id = ? AND doc_type IN (?)");
        assertThat(sql).doesNotContain("doc_id = ?");
    }

    private void stubQuery() {
        doReturn(List.of())
            .when(jdbcTemplate)
            .query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class));
    }

    private String capturedSql() {
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(jdbcTemplate)
            .query(sqlCaptor.capture(), any(PreparedStatementSetter.class), any(RowMapper.class));
        return sqlCaptor.getValue();
    }

    private float[] vector() {
        return new float[] {0.1f, 0.2f, 0.3f};
    }
}
