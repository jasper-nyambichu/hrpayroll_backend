-- src/main/resources/db/migration/V22__employee_payslips_and_company_logo.sql
--
-- employee_payslips — what each employee is due from one agent payment, saved when it is calculated:
--                     pay + PAYE + NSSF + NITA - loans - damages. The agent's amount is the sum of
--                     these + agent fee + VAT - advances.
-- companies         — the logo printed on payslips and employee lists. Kept in the database so it
--                     survives a redeploy of the backend container.

CREATE TABLE employee_payslips (
                                   id             BIGSERIAL PRIMARY KEY,
                                   created_at     TIMESTAMP,
                                   updated_at     TIMESTAMP,
                                   created_by     VARCHAR(255),
                                   updated_by     VARCHAR(255),

                                   settlement_id  BIGINT        NOT NULL REFERENCES agent_settlements(id),
                                   employee_id    BIGINT        NOT NULL REFERENCES employees(id),
                                   days_worked    INTEGER       NOT NULL,
                                   daily_rate     NUMERIC(15,2) NOT NULL,
                                   pay            NUMERIC(15,2) NOT NULL,
                                   paye           NUMERIC(15,2) NOT NULL,
                                   nssf           NUMERIC(15,2) NOT NULL,
                                   nita           NUMERIC(15,2) NOT NULL,
                                   loan           NUMERIC(15,2) NOT NULL,
                                   damage         NUMERIC(15,2) NOT NULL,
                                   amount_due     NUMERIC(15,2) NOT NULL,

                                   CONSTRAINT uk_employee_payslips_settlement_employee UNIQUE (settlement_id, employee_id)
);

CREATE INDEX idx_employee_payslips_employee ON employee_payslips(employee_id);

ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS logo_data         BYTEA,
    ADD COLUMN IF NOT EXISTS logo_content_type VARCHAR(20);
