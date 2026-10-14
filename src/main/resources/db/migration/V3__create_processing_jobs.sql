-- Flyway Migration V3: Create Processing Jobs and Processing Errors Tables
CREATE TABLE IF NOT EXISTS processing_jobs (
    id UUID PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    total_records INT DEFAULT 0,
    processed_records INT DEFAULT 0,
    successful_records INT DEFAULT 0,
    failed_records INT DEFAULT 0,
    duplicate_records INT DEFAULT 0,
    sequential_duration_ms BIGINT DEFAULT 0,
    parallel_duration_ms BIGINT DEFAULT 0,
    speedup_factor NUMERIC(6, 2) DEFAULT 0.00,
    throughput_records_per_sec NUMERIC(10, 2) DEFAULT 0.00,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS processing_errors (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES processing_jobs(id) ON DELETE CASCADE,
    row_number INT NOT NULL,
    entry_code VARCHAR(50),
    error_message TEXT NOT NULL,
    raw_data TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_errors_job_id ON processing_errors (job_id);
CREATE INDEX IF NOT EXISTS idx_jobs_status ON processing_jobs (status);
CREATE INDEX IF NOT EXISTS idx_jobs_created_at ON processing_jobs (created_at DESC);
