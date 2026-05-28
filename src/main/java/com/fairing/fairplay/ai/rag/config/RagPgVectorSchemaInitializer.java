package com.fairing.fairplay.ai.rag.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RagPgVectorSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Value("${rag.pgvector.auto-init:true}")
    private boolean autoInit;

    @Override
    public void run(ApplicationArguments args) {
        if (!autoInit) {
            log.info("pgvector RAG schema auto-init is disabled.");
            return;
        }

        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS rag_chunks (
                chunk_id varchar(128) PRIMARY KEY,
                doc_id varchar(128) NOT NULL,
                text text NOT NULL,
                embedding vector(768) NOT NULL,
                created_at timestamptz NOT NULL DEFAULT now(),
                updated_at timestamptz NOT NULL DEFAULT now()
            )
            """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunks_doc_id ON rag_chunks (doc_id)");
        jdbcTemplate.execute("ALTER TABLE rag_chunks ADD COLUMN IF NOT EXISTS doc_type varchar(64)");
        jdbcTemplate.execute("ALTER TABLE rag_chunks ADD COLUMN IF NOT EXISTS visibility varchar(32)");
        jdbcTemplate.execute("ALTER TABLE rag_chunks ADD COLUMN IF NOT EXISTS owner_user_id bigint");
        jdbcTemplate.execute("ALTER TABLE rag_chunks ADD COLUMN IF NOT EXISTS event_id bigint");
        jdbcTemplate.execute("ALTER TABLE rag_chunks ADD COLUMN IF NOT EXISTS booth_id bigint");
        jdbcTemplate.execute("ALTER TABLE rag_chunks ADD COLUMN IF NOT EXISTS reservation_id bigint");
        jdbcTemplate.execute("""
            UPDATE rag_chunks
            SET doc_type = CASE
                    WHEN doc_id LIKE 'event\\_%' ESCAPE '\\' THEN 'PUBLIC_EVENT'
                    WHEN doc_id LIKE 'booth\\_%' ESCAPE '\\' THEN 'PUBLIC_BOOTH'
                    WHEN doc_id LIKE 'booth_experience\\_%' ESCAPE '\\' THEN 'PUBLIC_BOOTH_EXPERIENCE'
                    WHEN doc_id LIKE 'review\\_%' ESCAPE '\\' THEN 'PUBLIC_REVIEW'
                    WHEN doc_id LIKE 'user\\_%' ESCAPE '\\' THEN 'USER_PROFILE'
                    WHEN doc_id LIKE 'reservation\\_%' ESCAPE '\\' THEN 'USER_RESERVATION'
                    ELSE COALESCE(doc_type, 'PUBLIC_MISC')
                END,
                visibility = CASE
                    WHEN doc_id LIKE 'user\\_%' ESCAPE '\\' OR doc_id LIKE 'reservation\\_%' ESCAPE '\\' THEN 'USER_PRIVATE'
                    ELSE COALESCE(visibility, 'PUBLIC')
                END,
                owner_user_id = CASE
                    WHEN doc_id LIKE 'user\\_%' ESCAPE '\\'
                    THEN NULLIF(regexp_replace(doc_id, '^user_', ''), '')::bigint
                    ELSE owner_user_id
                END
            WHERE doc_type IS NULL OR visibility IS NULL
            """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunks_scope ON rag_chunks (visibility, doc_type)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunks_owner_scope ON rag_chunks (owner_user_id, doc_type)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunks_event_id ON rag_chunks (event_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunks_reservation_id ON rag_chunks (reservation_id)");
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_rag_chunks_embedding_cosine
            ON rag_chunks USING ivfflat (embedding vector_cosine_ops)
            WITH (lists = 100)
            """);

        log.info("pgvector RAG schema is ready.");
    }
}
