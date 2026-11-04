-- SecuGen readers match on the branch PC, so a fingerprint has to reach that PC before it can be recognised
-- there. This is the one company-wide copy each branch PC pulls from, so nobody enrols again after a move.
-- A template is sensitive biometric data: the application encrypts it, and only the ciphertext is stored here.
CREATE TABLE employee_fingerprints (
    id                 BIGSERIAL PRIMARY KEY,
    employee_id        BIGINT    NOT NULL REFERENCES employees(id),
    finger_slot        SMALLINT  NOT NULL,
    template_encrypted BYTEA     NOT NULL,
    quality_score      INTEGER,
    enrolled_device_id BIGINT    REFERENCES biometric_devices(id),
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP NOT NULL DEFAULT now(),
    created_by         VARCHAR(255),
    updated_by         VARCHAR(255),

    CONSTRAINT uk_employee_finger UNIQUE (employee_id, finger_slot)
);

-- The company-wide lookup that lets a terminal id work at any branch.
CREATE INDEX idx_device_mappings_terminal_user_id ON biometric_device_mappings(terminal_user_id);
