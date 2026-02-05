-- Migration to add embedding column if vector extension is available
-- This handles the case where pgvector might not be installed

-- Try to create pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Add embedding column to business_document_chunks if it doesn't exist
ALTER TABLE IF EXISTS business_document_chunks
ADD COLUMN IF NOT EXISTS embedding vector;

-- Create index on embedding column if pgvector is available
-- If pgvector is not installed, the vector type won't be available
-- and this index creation will be skipped by database constraints
CREATE INDEX IF NOT EXISTS idx_doc_chunks_embedding
    ON business_document_chunks USING ivfflat (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

