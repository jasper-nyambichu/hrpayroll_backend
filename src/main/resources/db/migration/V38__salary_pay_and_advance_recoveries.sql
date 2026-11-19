-- Agent payments now pay each employee their own recorded monthly salary, prorated by the days paid (days worked
-- plus their off days), instead of days worked x one daily rate. Runs made before this keep their figures and are
-- still shown the way they were calculated: pay_basis tells the two apart.
ALTER TABLE agent_settlements ADD COLUMN IF NOT EXISTS pay_basis VARCHAR(20) NOT NULL DEFAULT 'DAILY_RATE';

ALTER TABLE employee_payslips ADD COLUMN IF NOT EXISTS monthly_salary NUMERIC(15, 2);
ALTER TABLE employee_payslips ADD COLUMN IF NOT EXISTS off_days_paid  INTEGER NOT NULL DEFAULT 0;

-- Which advances a payment run recovered, and how much from each, so the invoice can list every advance with the
-- date it was asked for, the date it was approved and the amount taken off.
CREATE TABLE settlement_advance_recoveries (
    id             BIGSERIAL PRIMARY KEY,
    settlement_id  BIGINT         NOT NULL REFERENCES agent_settlements(id),
    advance_id     BIGINT         NOT NULL REFERENCES agent_advances(id),
    amount         NUMERIC(15, 2) NOT NULL,
    balance_after  NUMERIC(15, 2) NOT NULL,
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    created_by     VARCHAR(255),
    updated_by     VARCHAR(255)
);
CREATE INDEX idx_settlement_advance_recoveries_settlement ON settlement_advance_recoveries(settlement_id);
