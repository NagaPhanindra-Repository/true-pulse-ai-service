-- Manual Setup Script for pgvector (Run if migrations don't work)
-- Execute this in pgAdmin or psql after installing pgvector

-- 1. Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Add embedding column to business_document_chunks if it doesn't exist
ALTER TABLE IF EXISTS business_document_chunks
ADD COLUMN IF NOT EXISTS embedding vector;

-- 3. Create indexes for better performance
-- Index on embedding column for similarity search
CREATE INDEX IF NOT EXISTS idx_doc_chunks_embedding
ON business_document_chunks USING ivfflat (embedding vector_cosine_ops);

-- 4. Alternative: Create HNSW index (faster for search, slower to build)
-- Uncomment if you prefer HNSW over IVFFlat
-- CREATE INDEX IF NOT EXISTS idx_doc_chunks_embedding_hnsw
-- ON business_document_chunks USING hnsw (embedding vector_cosine_ops);

-- 5. Verify everything is set up correctly
-- Check extension
SELECT * FROM pg_extension WHERE extname = 'vector';

-- Check column exists
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'business_document_chunks'
AND column_name = 'embedding';

-- Check indexes
SELECT indexname
FROM pg_indexes
WHERE tablename = 'business_document_chunks'
AND indexname LIKE '%embedding%';

-- 6. Optional: Set up pgvector configuration for better performance
-- These settings are for production optimization
-- ALTER SYSTEM SET max_parallel_workers_per_gather = 4;
-- SELECT pg_reload_conf();

-- 7. Check table size (includes embeddings)
SELECT
    pg_size_pretty(pg_total_relation_size('business_document_chunks')) AS size,
    (SELECT count(*) FROM business_document_chunks) AS row_count;

-- 8. Test vector operations (after you have data)
-- SELECT id, content, embedding <-> '[0.1, 0.2, 0.3, ...]'::vector AS distance
-- FROM business_document_chunks
-- ORDER BY distance
-- LIMIT 5;

-- Done! Your database is ready for embeddings.

