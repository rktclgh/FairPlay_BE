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
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_rag_chunks_embedding_cosine
            ON rag_chunks USING ivfflat (embedding vector_cosine_ops)
            WITH (lists = 100)
            """);

        log.info("pgvector RAG schema is ready.");
    }
}
