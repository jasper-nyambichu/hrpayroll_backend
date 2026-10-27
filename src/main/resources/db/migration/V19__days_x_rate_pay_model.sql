-- src/main/resources/db/migration/V19__days_x_rate_pay_model.sql
--
-- Phase 4: pay = days worked x daily rate, with SHIF and NSSF added on top.
--   company_settings  — the Admin-set daily rate, SHIF/NSSF (percentage of pay or fixed amount),
--                       the optional statutory switches (off by default) and the deduction ceiling.
--   payslips          — what the payslip now shows: days worked, daily rate, damage deductions and
--                       the total employee salary (pay + SHIF + NSSF).
--   employees         — the monthly figure is retired, so it is no longer required.

ALTER TABLE company_settings
    ADD COLUMN IF NOT EXISTS daily_rate               NUMERIC(15,2) NOT NULL DEFAULT 1000.00,
    ADD COLUMN IF NOT EXISTS shif_mode                VARCHAR(10)   NOT NULL DEFAULT 'PERCENT',
    ADD COLUMN IF NOT EXISTS shif_value               NUMERIC(15,4) NOT NULL DEFAULT 2.75,
    ADD COLUMN IF NOT EXISTS nssf_mode                VARCHAR(10)   NOT NULL DEFAULT 'PERCENT',
    ADD COLUMN IF NOT EXISTS nssf_value               NUMERIC(15,4) NOT NULL DEFAULT 6.00,
    ADD COLUMN IF NOT EXISTS apply_housing_levy       BOOLEAN       NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS apply_paye               BOOLEAN       NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS apply_nita               BOOLEAN       NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deduction_ceiling_percent NUMERIC(5,2) NOT NULL DEFAULT 66.67;

ALTER TABLE company_settings
    ADD CONSTRAINT chk_company_settings_shif_mode CHECK (shif_mode IN ('PERCENT', 'FIXED')),
    ADD CONSTRAINT chk_company_settings_nssf_mode CHECK (nssf_mode IN ('PERCENT', 'FIXED'));

ALTER TABLE payslips
    ADD COLUMN IF NOT EXISTS days_worked            INTEGER       NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS daily_rate             NUMERIC(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS damage_deduction       NUMERIC(15,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_employee_salary  NUMERIC(15,2);

-- Payslips from before this change were worked out the old way: their gross pay was the total.
UPDATE payslips SET damage_deduction = 0 WHERE damage_deduction IS NULL;
UPDATE payslips SET total_employee_salary = gross_pay WHERE total_employee_salary IS NULL;
ALTER TABLE payslips ALTER COLUMN total_employee_salary SET NOT NULL;

-- Staff are no longer given a monthly salary; payroll works from days worked and the daily rate.
ALTER TABLE employees ALTER COLUMN basic_salary DROP NOT NULL;