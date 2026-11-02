-- src/main/resources/db/migration/V21__agent_settlements_replace_payroll.sql
--
-- Payroll is terminated. The agent is paid one amount per period and pays their employees from it:
--   (days worked x daily rate) + PAYE + NSSF + NITA + VAT + agent fee - loans - damages - advances
--
-- agent_settlements      — one row per agent per period; replaces payroll_runs, payslips and
--                          agent_payroll_summaries, which are DROPPED here (no way back: back up first).
--                          Old agent statements are copied across, so a period that was already paid
--                          can never be calculated again.
-- company_settings       — VAT is 16%.
-- employees / requests   — position and employment type are no longer asked for, and salary not on requests.
-- users                  — only Admin, Branch Manager, Agent and Back Office accounts remain in use.
--                          The others are deactivated, not deleted, so their history keeps its names.

CREATE TABLE agent_settlements (
                                   id                    BIGSERIAL PRIMARY KEY,
                                   created_at            TIMESTAMP,
                                   updated_at            TIMESTAMP,
                                   created_by            VARCHAR(255),
                                   updated_by            VARCHAR(255),

                                   agent_id              BIGINT        NOT NULL REFERENCES agents(id),
                                   period_start          DATE          NOT NULL,
                                   period_end            DATE          NOT NULL,
                                   employee_count        INTEGER       NOT NULL,
                                   days_worked_total     INTEGER       NOT NULL,
                                   days_pay              NUMERIC(15,2) NOT NULL,
                                   paye_total            NUMERIC(15,2) NOT NULL,
                                   nssf_total            NUMERIC(15,2) NOT NULL,
                                   nita_total            NUMERIC(15,2) NOT NULL,
                                   agent_fee             NUMERIC(15,2) NOT NULL,
                                   vat_rate_used         NUMERIC(5,2)  NOT NULL,
                                   vat_amount            NUMERIC(15,2) NOT NULL,
                                   loan_total            NUMERIC(15,2) NOT NULL,
                                   damage_total          NUMERIC(15,2) NOT NULL,
                                   advance_recovered     NUMERIC(15,2) NOT NULL,
                                   amount_payable        NUMERIC(15,2) NOT NULL,
                                   payment_request_id    BIGINT        REFERENCES payment_requests(id),
                                   calculated_by_user_id BIGINT,

                                   CONSTRAINT uk_agent_settlements_agent_period UNIQUE (agent_id, period_start, period_end)
);

CREATE INDEX idx_agent_settlements_agent_id ON agent_settlements(agent_id);

INSERT INTO agent_settlements (created_at, updated_at, agent_id, period_start, period_end, employee_count,
                               days_worked_total, days_pay, paye_total, nssf_total, nita_total, agent_fee,
                               vat_rate_used, vat_amount, loan_total, damage_total, advance_recovered,
                               amount_payable, calculated_by_user_id)
SELECT s.created_at,
       s.updated_at,
       s.agent_id,
       r.period_start,
       r.period_end,
       COALESCE(s.employee_count, 0),
       COALESCE((SELECT SUM(p.days_worked)
                 FROM payslips p
                          JOIN employees e ON e.id = p.employee_id
                 WHERE p.payroll_run_id = s.payroll_run_id AND e.agent_id = s.agent_id), 0),
       s.gross_salaries,
       COALESCE(s.paye_total, 0),
       COALESCE(s.nssf_total, 0),
       COALESCE((SELECT SUM(p.nita_deduction)
                 FROM payslips p
                          JOIN employees e ON e.id = p.employee_id
                 WHERE p.payroll_run_id = s.payroll_run_id AND e.agent_id = s.agent_id), 0),
       s.service_charge,
       COALESCE(s.vat_rate_used, 0),
       COALESCE(s.vat_amount, 0),
       COALESCE(s.employee_loan_deductions_total, 0),
       0,
       COALESCE(s.advance_recovery_total, 0),
       s.final_amount_payable,
       r.initiated_by_user_id
FROM agent_payroll_summaries s
         JOIN payroll_runs r ON r.id = s.payroll_run_id
WHERE r.status = 'COMPLETED';

-- loan_repayments.payroll_run_id and damages.payroll_run_id keep their old run ids. New settlement ids
-- start above every old run id, so an old reference can never be mistaken for a settlement.
SELECT setval(pg_get_serial_sequence('agent_settlements', 'id'),
              GREATEST((SELECT COALESCE(MAX(id), 0) FROM agent_settlements),
                       (SELECT COALESCE(MAX(id), 0) FROM payroll_runs)) + 1,
              false);

DROP TABLE agent_payroll_summaries;
DROP TABLE payslips;
DROP TABLE payroll_runs;

UPDATE company_settings SET vat_rate = 16.00;

ALTER TABLE employees ALTER COLUMN employment_type DROP NOT NULL;
ALTER TABLE employee_creation_requests ALTER COLUMN employment_type DROP NOT NULL;
ALTER TABLE employee_creation_requests ALTER COLUMN basic_salary DROP NOT NULL;

UPDATE users SET active = FALSE
WHERE role IN ('HR_MANAGER', 'PAYROLL_OFFICER', 'FINANCE_OFFICER', 'AUDITOR', 'EMPLOYEE');
