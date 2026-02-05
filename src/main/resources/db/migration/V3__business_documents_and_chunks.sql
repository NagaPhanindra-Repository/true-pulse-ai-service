CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS business_documents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    business_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    file_name VARCHAR(255),
    file_type VARCHAR(100),
    file_size BIGINT,
    storage_path TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS business_document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    business_id VARCHAR(255) NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    prev_content TEXT,
    next_content TEXT,
    embedding_dimension INT NOT NULL,
    embedding VECTOR,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_business_documents_business_id
    ON business_documents (business_id);

CREATE INDEX IF NOT EXISTS idx_business_documents_user_id
    ON business_documents (user_id);

CREATE INDEX IF NOT EXISTS idx_doc_chunks_document_id
    ON business_document_chunks (document_id);

CREATE INDEX IF NOT EXISTS idx_doc_chunks_business_id
    ON business_document_chunks (business_id);

CREATE INDEX IF NOT EXISTS idx_doc_chunks_embedding
    ON business_document_chunks USING ivfflat (embedding vector_cosine_ops);

