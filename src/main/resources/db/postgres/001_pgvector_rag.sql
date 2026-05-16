CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS rag_chunks (
    chunk_id varchar(128) PRIMARY KEY,
    doc_id varchar(128) NOT NULL,
    text text NOT NULL,
    embedding vector(768) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_rag_chunks_doc_id
    ON rag_chunks (doc_id);

CREATE INDEX IF NOT EXISTS idx_rag_chunks_embedding_cosine
    ON rag_chunks USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
