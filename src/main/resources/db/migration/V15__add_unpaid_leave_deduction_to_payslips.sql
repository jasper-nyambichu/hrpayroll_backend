-- Unpaid leave taken out of gross pay for the period (approved leave of a type with paid = false).
ALTER TABLE payslips
    ADD COLUMN IF NOT EXISTS unpaid_leave_deduction NUMERIC(15,2) DEFAULT 0;

UPDATE payslips SET unpaid_leave_deduction = 0 WHERE unpaid_leave_deduction IS NULL;