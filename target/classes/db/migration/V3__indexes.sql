-- =====================================================
-- V3 — Indexes (HNSW vector, GIN full-text, B-tree)
-- =====================================================

-- HNSW index on catalog items embedding (semantic search)
CREATE INDEX catalog_embedding_hnsw ON catalog_items
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- HNSW index on PFE report chunks (RAG chatbot retrieval)
CREATE INDEX chunks_embedding_hnsw ON report_chunks
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- GIN index for keyword full-text search (French)
CREATE INDEX catalog_fts ON catalog_items
    USING GIN (to_tsvector('french',
        coalesce(title, '') || ' ' ||
        coalesce(author, '') || ' ' ||
        coalesce(subject, '')));

-- B-tree composite indexes for common query patterns
CREATE INDEX idx_loans_user_status ON loans (user_id, status);
CREATE INDEX idx_loans_due_status ON loans (due_at, status);
CREATE INDEX idx_purchases_user_item ON purchases (user_id, item_id);
CREATE INDEX idx_physical_copies_item_available ON physical_copies (item_id, is_available);
CREATE INDEX idx_reading_sessions_user_item ON reading_sessions (user_id, item_id);
CREATE INDEX idx_report_chunks_item ON report_chunks (item_id);
