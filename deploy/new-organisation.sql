-- =============================================================================
-- New organisation: run ONCE on a brand-new database, right after the backend's first start.
-- =============================================================================
--
-- The migrations V4, V5 and V7 seed demo rows (ADMIN, HR-MANAGER, PAYROLL-OFFICER, BRANCH-MANAGER,
-- FINANCE-OFFICER, AUDITOR and AGENT logins sharing a password that is in this repo, a "Head Office"
-- branch, a "Test Employee" and "Test Agent Ltd"). A new organisation must not start with those.
-- This script removes them, creates the organisation's company record and settings, and creates its
-- first login, SUPER-ADMIN, who then sets up branches, employees and every other login in the portal.
--
-- It REFUSES to run (and changes nothing) on a database that already has a company, a SUPER_ADMIN,
-- any employee other than the seeded one, or any attendance, so it cannot damage a live organisation.
--
-- Usage on the server (the password is typed, never stored in the shell history):
--   read -s -p "New SUPER-ADMIN password: " PW; echo
--   docker exec -i <stack>-postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
--       -v org_name="$1" -v admin_password="$2"' _ "SHAH CERAMICS" "$PW" < new-organisation.sql
--   unset PW
-- =============================================================================

\set ON_ERROR_STOP on

BEGIN;

-- psql variables are not visible inside DO blocks, so pass them through settings for this transaction.
-- Output is switched off so the password is never printed.
\o /dev/null
SELECT set_config('neworg.name', :'org_name', true),
       set_config('neworg.password', :'admin_password', true);
\o

DO $$
DECLARE
    org_name text := btrim(current_setting('neworg.name'));
    password text := current_setting('neworg.password');
BEGIN
    IF org_name = '' THEN
        RAISE EXCEPTION 'Organisation name is empty';
    END IF;
    IF length(password) < 10 THEN
        RAISE EXCEPTION 'Choose a SUPER-ADMIN password of at least 10 characters';
    END IF;

    -- Safety: only a database fresh from the migrations passes these.
    IF EXISTS (SELECT 1 FROM companies) THEN
        RAISE EXCEPTION 'REFUSED: this database already has a company (%). It is not a new organisation.',
            (SELECT string_agg(name, ', ') FROM companies);
    END IF;
    IF EXISTS (SELECT 1 FROM users WHERE role = 'SUPER_ADMIN') THEN
        RAISE EXCEPTION 'REFUSED: this database already has a SUPER_ADMIN login.';
    END IF;
    IF EXISTS (SELECT 1 FROM employees WHERE NOT (employee_number IN ('EMP-0001', 'EMP-1001') AND full_name = 'Test Employee')) THEN
        RAISE EXCEPTION 'REFUSED: this database has real employees.';
    END IF;
    IF EXISTS (SELECT 1 FROM attendance) THEN
        RAISE EXCEPTION 'REFUSED: this database has attendance records.';
    END IF;
END $$;

-- Remove the demo rows. Logins first: they point at the demo employee and agent.
DELETE FROM users
WHERE username IN ('ADMIN', 'HR-MANAGER', 'PAYROLL-OFFICER', 'BRANCH-MANAGER', 'FINANCE-OFFICER', 'AUDITOR', 'AGENT', 'EMP-1001', 'EMP-0001');
DELETE FROM employees WHERE full_name = 'Test Employee';
DELETE FROM agents WHERE name = 'Test Agent Ltd';
DELETE FROM branches WHERE name = 'Head Office' AND NOT EXISTS (SELECT 1 FROM employees);

-- The organisation. Name, contacts, logo and statutory numbers are then edited in Admin -> Settings.
INSERT INTO companies (name, currency, timezone, created_at, updated_at)
VALUES (upper(btrim(current_setting('neworg.name'))), 'KES', 'Africa/Nairobi', now(), now());

INSERT INTO company_settings (company_id, vat_rate, default_working_hours_per_day, overtime_multiplier,
                              weekend_days, payroll_period, created_at, updated_at)
SELECT id, 16.00, 8, 1.5, 'SATURDAY,SUNDAY', 'MONTHLY', now(), now() FROM companies;

-- The first login. bcrypt via pgcrypto, the same format the backend's password encoder reads.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
INSERT INTO users (username, password, role, display_name, active, must_change_password, created_at, updated_at)
VALUES ('SUPER-ADMIN', crypt(current_setting('neworg.password'), gen_salt('bf', 10)), 'SUPER_ADMIN',
        'Administrator', true, false, now(), now());
DROP EXTENSION pgcrypto;

-- What the organisation starts with: one login, one company, nothing else.
SELECT 'logins' AS what, string_agg(username || ' (' || role || ')', ', ') AS rows FROM users
UNION ALL SELECT 'company', string_agg(name, ', ') FROM companies
UNION ALL SELECT 'branches', count(*)::text FROM branches
UNION ALL SELECT 'employees', count(*)::text FROM employees
UNION ALL SELECT 'agents', count(*)::text FROM agents;

COMMIT;
