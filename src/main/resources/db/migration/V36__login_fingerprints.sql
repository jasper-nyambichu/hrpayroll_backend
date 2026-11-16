-- A Branch Manager (or a Back Office) need not be an employee of the branch, so their sign-in finger cannot hang
-- off an employee record. This is the finger of a LOGIN: it signs that login in on its own branch's PC and is never
-- used for attendance. One finger per login; registering again replaces it. Encrypted like employee_fingerprints.
CREATE TABLE login_fingerprints (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT    NOT NULL REFERENCES users(id),
    template_encrypted BYTEA     NOT NULL,
    quality_score      INTEGER,
    enrolled_device_id BIGINT    REFERENCES biometric_devices(id),
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP NOT NULL DEFAULT now(),
    created_by         VARCHAR(255),
    updated_by         VARCHAR(255),

    CONSTRAINT uk_login_fingerprint_user UNIQUE (user_id)
);
