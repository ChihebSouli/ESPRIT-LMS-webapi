-- =====================================================
-- V1 — Users and Catalog Items tables
-- =====================================================

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- =====================================================
-- Users (synced from ESPRIT central DB via auth MS)
-- =====================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    esprit_id       VARCHAR(50) UNIQUE NOT NULL,
    email           VARCHAR(255) UNIQUE NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'STUDENT'
                    CHECK (role IN ('STUDENT', 'TEACHER', 'LIBRARIAN', 'ADMIN')),
    matricule       VARCHAR(50),
    borrow_limit    INT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    last_synced_at  TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================================================
-- Catalog Items (books, articles, PFE reports)
-- =====================================================
CREATE TABLE catalog_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type            VARCHAR(20) NOT NULL
                    CHECK (type IN ('DIGITAL_BOOK', 'PHYSICAL_BOOK', 'PFE_REPORT', 'ARTICLE')),
    title           VARCHAR(500) NOT NULL,
    author          VARCHAR(500),
    subject         VARCHAR(255),
    language        VARCHAR(10) DEFAULT 'FR',
    year            INT,
    cover_image_url VARCHAR(1000),
    file_blob_key   VARCHAR(500),
    price           DECIMAL(8, 3),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    ai_summary      TEXT,
    ai_tags         TEXT[],
    embedding       vector(1024),
    ai_status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (ai_status IN ('PENDING', 'PROCESSING', 'COMPLETE', 'FAILED')),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
