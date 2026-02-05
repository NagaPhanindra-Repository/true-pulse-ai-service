-- Create business_document_chunks table with embedding column for pgvector

CREATE TABLE IF NOT EXISTS business_document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    business_id VARCHAR(255) NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    prev_content TEXT,
    next_content TEXT,
    embedding_dimension INTEGER NOT NULL DEFAULT 1536,
    embedding vector(1536),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES business_documents(id) ON DELETE CASCADE
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_doc_chunks_document_id ON business_document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_doc_chunks_business_id ON business_document_chunks(business_id);
CREATE INDEX IF NOT EXISTS idx_doc_chunks_created_at ON business_document_chunks(created_at);

-- Create HNSW index for vector similarity search (efficient for large datasets)
-- This improves performance for similarity searches
CREATE INDEX IF NOT EXISTS idx_doc_chunks_embedding ON business_document_chunks
USING hnsw (embedding vector_cosine_ops);

