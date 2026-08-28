CREATE TABLE biometric_sync_failures (
                                         id BIGSERIAL PRIMARY KEY,
                                         created_at TIMESTAMP,
                                         event_id VARCHAR(100),
                                         device_serial VARCHAR(100),
                                         employee_id BIGINT,
                                         reason VARCHAR(255) NOT NULL
);

CREATE INDEX idx_biometric_sync_failures_employee_id ON biometric_sync_failures(employee_id);
CREATE INDEX idx_biometric_sync_failures_created_at ON biometric_sync_failures(created_at);
ALTER TABLE attendance DROP COLUMN IF EXISTS match_confidence_score;
ALTER TABLE attendance ADD COLUMN verify_method VARCHAR(30);