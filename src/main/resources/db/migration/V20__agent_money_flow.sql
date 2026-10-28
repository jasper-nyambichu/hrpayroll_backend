-- src/main/resources/db/migration/V20__agent_money_flow.sql
--
-- Phase 5: the Agent money flow.
--   loans             — remembers which agent raised a loan for one of their employees.
--   agent_advances    — an agent can now REQUEST an advance (PENDING); the Admin approves it
--                       (it becomes OUTSTANDING and is recovered from payroll as before) or rejects it.
--   payment_requests  — an agent asks the Admin for money to pay their employees. The Admin approves,
--                       then the Accounts Office releases it.
--   employee_payments — each payment the agent makes to an employee from a released request,
--                       recorded with its M-Pesa transaction code (unique).

ALTER TABLE loans
    ADD COLUMN IF NOT EXISTS requested_by_agent_id BIGINT;

ALTER TABLE agent_advances
    ADD COLUMN IF NOT EXISTS requested_by_agent BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS decided_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS decided_at         TIMESTAMP,
    ADD COLUMN IF NOT EXISTS rejection_reason   VARCHAR(500);

CREATE TABLE payment_requests (
                                  id                   BIGSERIAL PRIMARY KEY,
                                  created_at           TIMESTAMP,
                                  updated_at           TIMESTAMP,
                                  created_by           VARCHAR(255),
                                  updated_by           VARCHAR(255),

                                  agent_id             BIGINT        NOT NULL REFERENCES agents(id),
                                  amount               NUMERIC(15,2) NOT NULL CHECK (amount > 0),
                                  purpose              VARCHAR(500)  NOT NULL,
                                  status               VARCHAR(20)   NOT NULL,   -- PENDING, APPROVED, REJECTED, RELEASED, CANCELLED
                                  requested_by_user_id BIGINT        NOT NULL,

                                  decided_by_user_id   BIGINT,
                                  decided_at           TIMESTAMP,
                                  rejection_reason     VARCHAR(500),

                                  released_by_user_id  BIGINT,
                                  released_at          TIMESTAMP,
                                  release_reference    VARCHAR(100)
);

CREATE INDEX idx_payment_requests_agent_id ON payment_requests(agent_id);
CREATE INDEX idx_payment_requests_status   ON payment_requests(status);

CREATE TABLE employee_payments (
                                   id                   BIGSERIAL PRIMARY KEY,
                                   created_at           TIMESTAMP,
                                   updated_at           TIMESTAMP,
                                   created_by           VARCHAR(255),
                                   updated_by           VARCHAR(255),

                                   payment_request_id   BIGINT        NOT NULL REFERENCES payment_requests(id),
                                   agent_id             BIGINT        NOT NULL REFERENCES agents(id),
                                   employee_id          BIGINT        NOT NULL REFERENCES employees(id),
                                   amount               NUMERIC(15,2) NOT NULL CHECK (amount > 0),
                                   mpesa_code           VARCHAR(20)   NOT NULL,
                                   paid_on              DATE          NOT NULL,
                                   note                 VARCHAR(255),
                                   recorded_by_user_id  BIGINT        NOT NULL,

                                   CONSTRAINT uk_employee_payments_mpesa_code UNIQUE (mpesa_code)
);

CREATE INDEX idx_employee_payments_request  ON employee_payments(payment_request_id);
CREATE INDEX idx_employee_payments_employee ON employee_payments(employee_id);
CREATE INDEX idx_employee_payments_agent    ON employee_payments(agent_id);
