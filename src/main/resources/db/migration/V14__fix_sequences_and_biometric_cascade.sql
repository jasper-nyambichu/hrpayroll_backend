-- V14__fix_sequences_and_biometric_cascade.sql
--
-- Fix 1: Sequence desync on the employees table.
--
-- V5 and V11 inserted seed rows directly into `employees` using plain INSERT
-- statements. PostgreSQL's BIGSERIAL sequence was never advanced by those
-- inserts, so it still starts at 1. The next call to employeeRepository.save()
-- asks the sequence for the next id, gets a value that already exists, and
-- throws:
--   duplicate key value violates unique constraint "employees_pkey"
--
-- setval(..., true) means "the sequence has already produced this value, so
-- the NEXT call returns max(id)+1". This is the standard PostgreSQL idiom for
-- resyncing a sequence after bulk inserts that bypassed it.
--
-- The same desync can affect any table that received seed rows, so we reset
-- every BIGSERIAL sequence in one migration.

SELECT setval(pg_get_serial_sequence('employees',          'id'), COALESCE(MAX(id), 0) + 1, false) FROM employees;
SELECT setval(pg_get_serial_sequence('users',              'id'), COALESCE(MAX(id), 0) + 1, false) FROM users;
SELECT setval(pg_get_serial_sequence('branches',           'id'), COALESCE(MAX(id), 0) + 1, false) FROM branches;
SELECT setval(pg_get_serial_sequence('departments',        'id'), COALESCE(MAX(id), 0) + 1, false) FROM departments;
SELECT setval(pg_get_serial_sequence('agents',             'id'), COALESCE(MAX(id), 0) + 1, false) FROM agents;
SELECT setval(pg_get_serial_sequence('agent_advances',     'id'), COALESCE(MAX(id), 0) + 1, false) FROM agent_advances;
SELECT setval(pg_get_serial_sequence('companies',          'id'), COALESCE(MAX(id), 0) + 1, false) FROM companies;
SELECT setval(pg_get_serial_sequence('company_settings',   'id'), COALESCE(MAX(id), 0) + 1, false) FROM company_settings;
SELECT setval(pg_get_serial_sequence('biometric_devices',  'id'), COALESCE(MAX(id), 0) + 1, false) FROM biometric_devices;
SELECT setval(pg_get_serial_sequence('attendance',         'id'), COALESCE(MAX(id), 0) + 1, false) FROM attendance;
SELECT setval(pg_get_serial_sequence('shifts',             'id'), COALESCE(MAX(id), 0) + 1, false) FROM shifts;
SELECT setval(pg_get_serial_sequence('shift_assignments',  'id'), COALESCE(MAX(id), 0) + 1, false) FROM shift_assignments;
SELECT setval(pg_get_serial_sequence('leave_types',        'id'), COALESCE(MAX(id), 0) + 1, false) FROM leave_types;
SELECT setval(pg_get_serial_sequence('leave_requests',     'id'), COALESCE(MAX(id), 0) + 1, false) FROM leave_requests;
SELECT setval(pg_get_serial_sequence('loans',              'id'), COALESCE(MAX(id), 0) + 1, false) FROM loans;
SELECT setval(pg_get_serial_sequence('loan_repayments',    'id'), COALESCE(MAX(id), 0) + 1, false) FROM loan_repayments;
SELECT setval(pg_get_serial_sequence('payroll_runs',       'id'), COALESCE(MAX(id), 0) + 1, false) FROM payroll_runs;
SELECT setval(pg_get_serial_sequence('payslips',           'id'), COALESCE(MAX(id), 0) + 1, false) FROM payslips;
SELECT setval(pg_get_serial_sequence('notifications',      'id'), COALESCE(MAX(id), 0) + 1, false) FROM notifications;
SELECT setval(pg_get_serial_sequence('audit_logs',         'id'), COALESCE(MAX(id), 0) + 1, false) FROM audit_logs;
SELECT setval(pg_get_serial_sequence('employee_documents', 'id'), COALESCE(MAX(id), 0) + 1, false) FROM employee_documents;


-- Fix 2: Biometric mappings block employee deletion.
--
-- biometric_device_mappings.employee_id has no referential action, so trying
-- to DELETE an employee that has been enrolled on a device either:
--   (a) fails with a FK violation if the constraint exists, or
--   (b) leaves orphaned mapping rows that will cause "employee not found"
--       errors on the next kiosk sync.
--
-- Adding ON DELETE CASCADE means: when an employee row is deleted, all of
-- their device mappings are automatically removed. The biometric device itself
-- is unaffected — only the mapping record (the translation between terminal
-- user id and employee id) is cleaned up. The terminal's on-device fingerprint
-- template is managed by the ZKTeco device, not by this database, so nothing
-- on the hardware changes.
--
-- We also add the same cascade to attendance and shift_assignments so that
-- deleting an employee doesn't leave orphaned history rows that would break
-- payroll queries or attendance reports.

-- biometric_device_mappings
ALTER TABLE biometric_device_mappings
    DROP CONSTRAINT IF EXISTS biometric_device_mappings_employee_id_fkey;

ALTER TABLE biometric_device_mappings
    ADD CONSTRAINT biometric_device_mappings_employee_id_fkey
        FOREIGN KEY (employee_id)
            REFERENCES employees (id)
            ON DELETE CASCADE;

-- attendance
ALTER TABLE attendance
    DROP CONSTRAINT IF EXISTS attendance_employee_id_fkey;

ALTER TABLE attendance
    ADD CONSTRAINT attendance_employee_id_fkey
        FOREIGN KEY (employee_id)
            REFERENCES employees (id)
            ON DELETE CASCADE;

-- shift_assignments
ALTER TABLE shift_assignments
    DROP CONSTRAINT IF EXISTS shift_assignments_employee_id_fkey;

ALTER TABLE shift_assignments
    ADD CONSTRAINT shift_assignments_employee_id_fkey
        FOREIGN KEY (employee_id)
            REFERENCES employees (id)
            ON DELETE CASCADE;
