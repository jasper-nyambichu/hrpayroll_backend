-- src/main/resources/db/migration/V18__create_damages_and_days_worked.sql
--
-- Phase 3 (Back Office):
--   damages              — a damage recorded against an employee. It is approved by the employee's
--                          fingerprint at the clock-in device, or overridden by the officer with a
--                          mandatory reason (an override is flagged for good). Payroll deducts APPROVED rows.
--   days_worked_records  — the locked count of days an employee reported to work in a pay period.
--                          Payroll reads this instead of a monthly salary (Phase 4).

CREATE TABLE damages (
                         id                    BIGSERIAL PRIMARY KEY,
                         created_at            TIMESTAMP,
                         updated_at            TIMESTAMP,
                         created_by            VARCHAR(255),
                         updated_by            VARCHAR(255),

                         employee_id           BIGINT        NOT NULL REFERENCES employees(id),
                         branch_id             BIGINT        NOT NULL REFERENCES branches(id),
                         amount                NUMERIC(15,2) NOT NULL CHECK (amount > 0),
                         description           VARCHAR(500)  NOT NULL,
                         damage_date           DATE          NOT NULL,
                         status                VARCHAR(20)   NOT NULL,   -- PENDING_CONSENT, APPROVED, CANCELLED, DEDUCTED
                         recorded_by_user_id   BIGINT        NOT NULL,

                         approval_method       VARCHAR(20),              -- FINGERPRINT or OVERRIDE
                         consent_at            TIMESTAMP,
                         consent_device_id     BIGINT,
                         consent_event_id      VARCHAR(100),

                         overridden            BOOLEAN       NOT NULL DEFAULT FALSE,
                         override_reason       VARCHAR(500),
                         overridden_by_user_id BIGINT,
                         overridden_at         TIMESTAMP,

                         payroll_run_id        BIGINT,
                         deducted_at           TIMESTAMP,

                         CONSTRAINT uk_damages_consent_event UNIQUE (consent_event_id)
);

CREATE INDEX idx_damages_employee_id ON damages(employee_id);
CREATE INDEX idx_damages_branch_id   ON damages(branch_id);
CREATE INDEX idx_damages_status      ON damages(status);

CREATE TABLE days_worked_records (
                                     id                  BIGSERIAL PRIMARY KEY,
                                     created_at          TIMESTAMP,
                                     updated_at          TIMESTAMP,
                                     created_by          VARCHAR(255),
                                     updated_by          VARCHAR(255),

                                     employee_id         BIGINT       NOT NULL REFERENCES employees(id),
                                     branch_id           BIGINT       NOT NULL REFERENCES branches(id),
                                     period_start        DATE         NOT NULL,
                                     period_end          DATE         NOT NULL,
                                     system_days         INTEGER      NOT NULL CHECK (system_days >= 0),
                                     days_worked         INTEGER      NOT NULL CHECK (days_worked >= 0),
                                     adjusted            BOOLEAN      NOT NULL DEFAULT FALSE,
                                     adjustment_reason   VARCHAR(500),
                                     locked_by_user_id   BIGINT       NOT NULL,
                                     locked_at           TIMESTAMP    NOT NULL,

                                     CONSTRAINT uk_days_worked_employee_period UNIQUE (employee_id, period_start, period_end),
                                     CONSTRAINT chk_days_worked_period CHECK (period_end >= period_start)
);

CREATE INDEX idx_days_worked_employee_id ON days_worked_records(employee_id);
CREATE INDEX idx_days_worked_branch_period ON days_worked_records(branch_id, period_start, period_end);
