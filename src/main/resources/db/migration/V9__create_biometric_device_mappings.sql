-- src/main/resources/db/migration/V9__create_biometric_device_mappings.sql

CREATE TABLE biometric_device_mappings (
                                           id               BIGSERIAL PRIMARY KEY,
                                           device_id        BIGINT       NOT NULL REFERENCES biometric_devices(id),
                                           terminal_user_id VARCHAR(20)  NOT NULL,
                                           employee_id      BIGINT       NOT NULL REFERENCES employees(id),
                                           enrolled_at      TIMESTAMP    NOT NULL,
                                           created_at       TIMESTAMP    NOT NULL DEFAULT now(),
                                           updated_at       TIMESTAMP    NOT NULL DEFAULT now(),
                                           created_by       VARCHAR(255),
                                           updated_by       VARCHAR(255),

                                           CONSTRAINT uk_device_terminal_user UNIQUE (device_id, terminal_user_id)
);

CREATE INDEX idx_device_mappings_device_id ON biometric_device_mappings(device_id);
CREATE INDEX idx_device_mappings_employee_id ON biometric_device_mappings(employee_id);