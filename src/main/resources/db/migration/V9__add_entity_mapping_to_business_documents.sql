ALTER TABLE business_documents
    ADD COLUMN IF NOT EXISTS entity_id BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(255) NOT NULL DEFAULT '';

ALTER TABLE business_document_chunks
    ADD COLUMN IF NOT EXISTS entity_id BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(255) NOT NULL DEFAULT '';

CREATE INDEX IF NOT EXISTS idx_business_documents_entity_id ON business_documents(entity_id);
CREATE INDEX IF NOT EXISTS idx_business_documents_display_name ON business_documents(display_name);
CREATE INDEX IF NOT EXISTS idx_doc_chunks_entity_id ON business_document_chunks(entity_id);
CREATE INDEX IF NOT EXISTS idx_doc_chunks_display_name ON business_document_chunks(display_name);

