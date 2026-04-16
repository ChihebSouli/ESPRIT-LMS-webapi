-- =====================================================
-- V2 — Physical copies, Loans, Purchases, Reader,
--       Report chunks, Recommendations, Config, Email
-- =====================================================

-- =====================================================
-- Physical Copies (for physical books)
-- =====================================================
CREATE TABLE physical_copies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id         UUID NOT NULL REFERENCES catalog_items(id) ON DELETE CASCADE,
    barcode         VARCHAR(100) UNIQUE NOT NULL,
    condition       VARCHAR(20) NOT NULL DEFAULT 'GOOD'
                    CHECK (condition IN ('NEW', 'GOOD', 'WORN', 'DAMAGED')),
    is_available    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================================================
-- Loans (reserve → borrow → return lifecycle)
-- =====================================================
CREATE TABLE loans (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id),
    copy_id                 UUID NOT NULL REFERENCES physical_copies(id),
    reserved_at             TIMESTAMP NOT NULL DEFAULT NOW(),
    borrowed_at             TIMESTAMP,
    due_at                  TIMESTAMP,
    returned_at             TIMESTAMP,
    renewed                 BOOLEAN NOT NULL DEFAULT FALSE,
    status                  VARCHAR(20) NOT NULL DEFAULT 'RESERVED'
                            CHECK (status IN ('RESERVED', 'ACTIVE', 'OVERDUE', 'RETURNED', 'CANCELLED')),
    expiry_reminder_sent    BOOLEAN NOT NULL DEFAULT FALSE,
    overdue_reminder_sent   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================================================
-- Purchases (online + desk payments)
-- =====================================================
CREATE TABLE purchases (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id),
    item_id             UUID NOT NULL REFERENCES catalog_items(id),
    amount              DECIMAL(8, 3) NOT NULL,
    payment_method      VARCHAR(20) NOT NULL
                        CHECK (payment_method IN ('FLOUCI', 'STRIPE', 'DESK')),
    payment_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED')),
    gateway_session_id  VARCHAR(500),
    transaction_ref     VARCHAR(500),
    purchased_at        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================================================
-- Reading Sessions (MinIO presigned URL DRM)
-- =====================================================
CREATE TABLE reading_sessions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id),
    item_id     UUID NOT NULL REFERENCES catalog_items(id),
    minio_token VARCHAR(500),
    last_page   INT NOT NULL DEFAULT 1,
    started_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMP NOT NULL
);

-- =====================================================
-- Report Chunks (PFE report text chunks for RAG)
-- =====================================================
CREATE TABLE report_chunks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id     UUID NOT NULL REFERENCES catalog_items(id) ON DELETE CASCADE,
    chunk_idx   INT NOT NULL,
    chunk_text  TEXT NOT NULL,
    embedding   vector(1024)
);

-- =====================================================
-- User Recommendations (pre-computed nightly)
-- =====================================================
CREATE TABLE user_recommendations (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    item_id     UUID NOT NULL REFERENCES catalog_items(id) ON DELETE CASCADE,
    score       FLOAT NOT NULL,
    rank        INT NOT NULL,
    computed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, item_id)
);

-- =====================================================
-- System Configuration (key-value settings)
-- =====================================================
CREATE TABLE system_config (
    key     VARCHAR(100) PRIMARY KEY,
    value   VARCHAR(500) NOT NULL
);

-- Seed default configuration values
INSERT INTO system_config (key, value) VALUES
    ('loan_duration_days', '14'),
    ('renewal_days', '7'),
    ('default_borrow_limit', '3'),
    ('reservation_expiry_hours', '48');

-- =====================================================
-- Email Templates
-- =====================================================
CREATE TABLE email_templates (
    key         VARCHAR(100) PRIMARY KEY,
    subject     VARCHAR(500) NOT NULL,
    html_body   TEXT NOT NULL
);
