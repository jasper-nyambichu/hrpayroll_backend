-- V32__lookup_indexes.sql
-- Indexes for lookups the system runs on every page and every payment run. Adds indexes only; no data changes.
--   audit_logs: the audit list (newest first), one record's history, and one user's actions
--   damages / loan_repayments: what each payment run deducted (itemised payslips)
--   users: logins of a branch, and the login linked to an employee (fingerprint sign-in, staff roles)
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_performed_by ON audit_logs (performed_by_user_id);
CREATE INDEX IF NOT EXISTS idx_damages_payroll_run ON damages (payroll_run_id);
CREATE INDEX IF NOT EXISTS idx_loan_repayments_payroll_run ON loan_repayments (payroll_run_id);
CREATE INDEX IF NOT EXISTS idx_users_branch ON users (branch_id);
CREATE INDEX IF NOT EXISTS idx_users_employee ON users (employee_id);
