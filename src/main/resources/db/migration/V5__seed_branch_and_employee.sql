-- Branch (employees.branch_id is NOT NULL, so this must exist first)
INSERT INTO branches (name, code, status, created_at, updated_at)
VALUES ('Head Office', 'HQ-001', 'ACTIVE', now(), now());

-- Employee, linked to the branch above
INSERT INTO employees (
    full_name, employee_number, branch_id,
    employment_type, basic_salary, employment_status,
    created_at, updated_at
) VALUES (
             'Test Employee', 'EMP-1001',
             (SELECT id FROM branches WHERE code = 'HQ-001'),
             'FULL_TIME', 30000.00, 'ACTIVE',
             now(), now()
         );

-- User account linked to the employee above (PIN = users.password)
INSERT INTO users (
    username, password, role, display_name,
    employee_id, active, must_change_password,
    created_at, updated_at
) VALUES (
             'EMP-1001',
             '$2b$10$e76DMykFVRjiV3mYhlxvGuxvLeNbEzY9gmgQddwb/OWmHi96sdrt.',
             'EMPLOYEE', 'Test Employee',
             (SELECT id FROM employees WHERE employee_number = 'EMP-1001'),
             true, false,
             now(), now()
         );