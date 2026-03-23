-- V1__create_transcriptions_table.sql
-- Initial schema for the transcriptions table

CREATE TABLE transcriptions (
    id                      BIGSERIAL PRIMARY KEY,
    filename                VARCHAR(500) NOT NULL,
    original_file_type      VARCHAR(100),
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    transcript_text         TEXT,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    audio_duration_seconds  DOUBLE PRECISION,
    error_message           TEXT,
    stored_file_path        VARCHAR(1000)
);

CREATE INDEX idx_transcriptions_status ON transcriptions(status);
CREATE INDEX idx_transcriptions_created_at ON transcriptions(created_at DESC);
