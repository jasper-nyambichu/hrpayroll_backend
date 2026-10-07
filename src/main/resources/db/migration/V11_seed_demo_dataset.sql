-- ============================================================
-- V11__seed_demo_dataset.sql
--
-- A full demo dataset so every portal has real rows to render.
-- Before this migration the database held 1 branch, 1 employee,
-- 1 agent and 8 users — enough to prove the API works, not enough
-- to look at a dashboard.
--
-- Contents: company + settings, 6 extra branches, 14 departments,
-- 2 extra agents, 40 employees, 3 documents per employee, a
-- SUPER_ADMIN account, 6 leave types, leave requests, shifts +
-- assignments, a biometric terminal per branch, 30 days of
-- attendance, one COMPLETED payroll run with payslips and agent
-- summaries, one IN_PROGRESS run, loans, advances, notifications
-- and audit events.
--
-- ── THREE RULES THIS FILE FOLLOWS ────────────────────────────
--
-- 1. EVERY VALUE MATCHES THE JAVA ENTITY. Columns like
--    `attendance.status` are plain VARCHAR with no CHECK
--    constraint, so a typo'd status inserts happily and only
--    explodes later when Hibernate reads it back. Valid values
--    are taken from the `// comment` beside each field in
--    `entity/**`. `EmploymentType` and `ShiftType` are real Java
--    enums, so those strings must match the constants exactly.
--
--    A consequence worth knowing: every row below is inserted with
--    a JOIN onto the table it references (`branches.code`,
--    `employees.employee_number`, `leave_types.name`, …). If a
--    referenced value is misspelled the row is silently dropped
--    rather than failing the migration — so those joins are doing
--    double duty as validation.
--
-- 2. DERIVED NUMBERS USE THE PRODUCTION FORMULAS. SHIF, NSSF,
--    Housing Levy and PAYE reproduce `StatutoryDeductionCalculator`
--    line for line, and overtime reproduces
--    `PayrollCalculationServiceImpl`. A payslip seeded here would
--    be recomputed to the same figure by a real run — the demo
--    data does not contradict the engine.
--
-- 3. NOTHING BLOCKS THE WORKING KIOSK PATH. `EMP-1001` (the
--    employee the desktop terminal is enrolled against) gets NO
--    attendance row for today: `BiometricSyncServiceImpl
--    .handleClockIn` rejects a second clock-in when an open
--    session already exists, so seeding one would break the
--    Agenda #1 demo. `SECUGEN-BRANCH1-003` is likewise never
--    touched — the new devices use distinct serials.
--
-- Every insert is guarded by `ON CONFLICT` on a natural key or a
-- `WHERE NOT EXISTS`, so re-running against a partially seeded
-- database adds what is missing instead of failing.
--
-- NOTE ON DATES: everything is relative to the run date, so the
-- dashboard always shows a fresh 30-day window and a payroll run
-- for "last month" — wherever "now" happens to be. The company
-- timezone is Africa/Nairobi, matching `BusinessClock`.
-- ============================================================

-- ------------------------------------------------------------
-- 1. Company
-- ------------------------------------------------------------
INSERT INTO companies (
    name, logo_url, address, phone, email, website,
    kra_pin, shif_number, nssf_number, nita_number, currency, timezone,
    created_at, updated_at
)
SELECT
    'Dhwarsh HR Solutions Ltd',
    NULL,
    'Kimathi Street, Nairobi CBD, Nairobi',
    '+254 20 271 4400',
    'info@dhwarsh.co.ke',
    'https://dhwarsh.co.ke',
    'P051234567X',
    'SHIF-0001234567',
    'NSSF-10023456',
    'NITA/TRN/4812',
    'KES',
    'Africa/Nairobi',
    now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM companies);

-- ------------------------------------------------------------
-- 2. Company settings — drives VAT, working hours and overtime.
--    `vat_rate` is read by the agent invoice formula, so this row
--    is the difference between a 16% invoice and a 0% one.
-- ------------------------------------------------------------
INSERT INTO company_settings (
    company_id, vat_rate, default_working_hours_per_day,
    overtime_multiplier, weekend_days, payroll_period,
    created_at, updated_at
)
SELECT
    (SELECT id FROM companies ORDER BY id LIMIT 1),
    16.00, 8.0, 1.5, 'SATURDAY,SUNDAY', 'MONTHLY',
    now(), now()
WHERE NOT EXISTS (SELECT 1 FROM company_settings);

-- ------------------------------------------------------------
-- 3. Branches — HQ-001 already exists from V5 and is reused.
-- ------------------------------------------------------------
INSERT INTO branches (name, code, address, phone, email, status, created_at, updated_at)
VALUES
    ('Nairobi CBD',       'NBO-002', 'Kenyatta Avenue, Nairobi', '+254 20 271 4401', 'nairobi@dhwarsh.co.ke',   'ACTIVE', now(), now()),
    ('Mombasa Port',      'MSA-003', 'Moi Avenue, Mombasa',      '+254 41 222 3301', 'mombasa@dhwarsh.co.ke',   'ACTIVE', now(), now()),
    ('Kisumu Industrial', 'KSM-004', 'Industrial Area, Kisumu',  '+254 57 202 1102', 'kisumu@dhwarsh.co.ke',    'ACTIVE', now(), now()),
    ('Nakuru',            'NKR-005', 'Kenyatta Avenue, Nakuru',  '+254 51 221 4403', 'nakuru@dhwarsh.co.ke',    'ACTIVE', now(), now()),
    ('Thika Road',        'THK-006', 'Thika Road Mall, Nairobi', '+254 20 271 4404', 'thikaroad@dhwarsh.co.ke', 'ACTIVE', now(), now()),
    ('Eldoret Depot',     'ELD-007', 'Kenyatta Street, Eldoret', '+254 53 206 1105', 'eldoret@dhwarsh.co.ke',   'ACTIVE', now(), now())
    ON CONFLICT (code) DO NOTHING;

-- ------------------------------------------------------------
-- 4. Departments — 14 across the 7 branches.
--    No unique constraint on (name, branch_id), so guard with
--    NOT EXISTS rather than ON CONFLICT.
-- ------------------------------------------------------------
INSERT INTO departments (name, branch_id, created_at, updated_at)
SELECT v.name, b.id, now(), now()
FROM (VALUES
          ('Human Resources',       'HQ-001'),
          ('Finance & Accounts',    'HQ-001'),
          ('Information Technology','HQ-001'),
          ('Payroll & Benefits',    'HQ-001'),
          ('Operations',            'HQ-001'),
          ('Sales & Marketing',     'NBO-002'),
          ('Customer Service',      'NBO-002'),
          ('Warehouse',             'MSA-003'),
          ('Clearing & Forwarding', 'MSA-003'),
          ('Production',            'KSM-004'),
          ('Quality Assurance',     'KSM-004'),
          ('Agriculture Operations','NKR-005'),
          ('Retail Operations',     'THK-006'),
          ('Logistics',             'ELD-007')
     ) AS v(name, branch_code)
         JOIN branches b ON b.code = v.branch_code
WHERE NOT EXISTS (
    SELECT 1 FROM departments d WHERE d.name = v.name AND d.branch_id = b.id
);

-- ------------------------------------------------------------
-- 5. Labour agents — one already exists from V7. Two more so the
--    agent portal and the outsourcing split have something to
--    group by. Charge types must match the ChargeType enum.
--    `agents.name` is not UNIQUE, so ON CONFLICT is unavailable.
-- ------------------------------------------------------------
INSERT INTO agents (
    name, contact_person, phone, email, address,
    bank_name, bank_account_number, kra_pin,
    contract_start_date, contract_end_date,
    charge_type, charge_amount, charge_percentage,
    vat_registered, status, created_at, updated_at
)
SELECT v.name, v.contact, v.phone, v.email, v.address,
       v.bank, v.account, v.kra,
       v.contract_start, v.contract_end,
       v.charge_type, v.charge_amount, v.charge_percentage,
       v.vat_registered, 'ACTIVE', now(), now()
FROM (VALUES
          ('Mavuno Staffing Services', 'Alice Muthoni', '+254 722 400 118', 'accounts@mavunostaffing.co.ke',
           'Ngong Road, Nairobi', 'Equity Bank', '0170293847561', 'P051987654Y',
           DATE '2024-01-01', DATE '2027-12-31', 'PERCENTAGE_OF_GROSS', 0.00, 10.00, true),
          ('Coastal Manpower Ltd', 'Yusuf Athman', '+254 733 512 907', 'billing@coastalmanpower.co.ke',
           'Nkrumah Road, Mombasa', 'KCB Bank', '1102938475601', 'P051876543Z',
           DATE '2024-06-01', DATE '2026-05-31', 'PER_EMPLOYEE', 3500.00, 0.00, true)
     ) AS v(name, contact, phone, email, address, bank, account, kra,
            contract_start, contract_end, charge_type, charge_amount, charge_percentage, vat_registered)
WHERE NOT EXISTS (SELECT 1 FROM agents a WHERE a.name = v.name);

-- ------------------------------------------------------------
-- 6. Employees — 40 spread across the 7 branches.
--
-- Government numbers, bank accounts and emergency phone numbers
-- are derived from the employee number's numeric part, so every
-- row is unique without hand-writing 40 KRA PINs.
-- `employees.national_id` is UNIQUE, and a derived value is what
-- makes a collision impossible.
-- ------------------------------------------------------------
INSERT INTO employees (
    full_name, employee_number, national_id, date_of_birth, gender, marital_status,
    phone, email, address,
    kra_pin, shif_number, nssf_number, nita_number,
    branch_id, department_id, agent_id, position,
    employment_type, basic_salary, date_hired, employment_status,
    bank_name, bank_account_number,
    emergency_contact_name, emergency_contact_phone,
    created_at, updated_at
)
SELECT
    v.full_name,
    v.employee_number,
    '2' || lpad(n.seq::text, 7, '0'),
    v.date_of_birth,
    v.gender,
    v.marital_status,
    v.phone,
    lower(replace(v.full_name, ' ', '.')) || '@dhwarsh.co.ke',
    b.name || ', Kenya',
    'A' || lpad(n.seq::text, 9, '0') || 'Z',
    'SHIF' || lpad(n.seq::text, 8, '0'),
    'NSSF' || lpad(n.seq::text, 8, '0'),
    'NITA' || lpad(n.seq::text, 8, '0'),
    b.id,
    d.id,
    CASE WHEN v.employment_type = 'OUTSOURCED' THEN
             CASE v.branch_code
                 WHEN 'NBO-002' THEN (SELECT id FROM agents WHERE name = 'Test Agent Ltd')
                 WHEN 'MSA-003' THEN (SELECT id FROM agents WHERE name = 'Coastal Manpower Ltd')
                 ELSE (SELECT id FROM agents WHERE name = 'Mavuno Staffing Services')
                 END
        END,
    v.position,
    v.employment_type,
    v.basic_salary,
    v.date_hired,
    v.employment_status,
    v.bank_name,
    lpad(n.seq::text, 10, '0'),
    v.emergency_contact_name,
    '+2547' || lpad(n.seq::text, 8, '0'),
    now(), now()
FROM (VALUES
          -- ── Head Office ──────────────────────────────────────────
          ('Peter Njuguna',     'EMP-00412', DATE '1985-03-14', 'MALE',   'MARRIED', '+254 722 345 101', 'HR Manager',                 'PERMANENT',  250000.00, DATE '2016-04-01', 'ACTIVE',     'HQ-001', 'Human Resources',         'KCB Bank',           'Mary Njuguna'),
          ('Grace Wambui',      'EMP-00413', DATE '1990-07-22', 'FEMALE', 'MARRIED', '+254 722 345 102', 'HR Officer',                 'PERMANENT',   95000.00, DATE '2018-09-03', 'ACTIVE',     'HQ-001', 'Human Resources',         'Equity Bank',        'Samuel Wambui'),
          ('John Kamau',        'EMP-00414', DATE '1983-11-05', 'MALE',   'MARRIED', '+254 722 345 103', 'Finance Manager',            'PERMANENT',  220000.00, DATE '2014-02-17', 'ACTIVE',     'HQ-001', 'Finance & Accounts',      'KCB Bank',           'Alice Kamau'),
          ('Faith Njeri',       'EMP-00415', DATE '1992-01-30', 'FEMALE', 'SINGLE',  '+254 722 345 104', 'Accountant',                 'PERMANENT',  105000.00, DATE '2019-06-10', 'ACTIVE',     'HQ-001', 'Finance & Accounts',      'NCBA Bank',          'Daniel Mwangi'),
          ('Brian Ochieng',     'EMP-00416', DATE '1994-05-18', 'MALE',   'SINGLE',  '+254 722 345 105', 'Software Engineer',          'PERMANENT',  155000.00, DATE '2021-03-01', 'ACTIVE',     'HQ-001', 'Information Technology',  'Equity Bank',        'Millicent Achieng'),
          ('Cynthia Chebet',    'EMP-00417', DATE '1996-09-12', 'FEMALE', 'SINGLE',  '+254 722 345 106', 'IT Support Officer',         'CONTRACT',    78000.00, DATE '2023-01-09', 'ACTIVE',     'HQ-001', 'Information Technology',  'Co-operative Bank',  'Kevin Kiplagat'),
          ('Samuel Kiprop',     'EMP-00418', DATE '1988-12-01', 'MALE',   'MARRIED', '+254 722 345 107', 'Payroll Officer',            'PERMANENT',   92000.00, DATE '2017-08-14', 'ACTIVE',     'HQ-001', 'Payroll & Benefits',      'KCB Bank',           'Ruth Kiprop'),
          ('Esther Mueni',      'EMP-00419', DATE '1993-04-27', 'FEMALE', 'MARRIED', '+254 722 345 108', 'Payroll Assistant',          'CONTRACT',    62000.00, DATE '2022-05-16', 'ACTIVE',     'HQ-001', 'Payroll & Benefits',      'Absa Bank',          'Joseph Mutua'),
          ('Daniel Mwangi',     'EMP-00420', DATE '1981-08-09', 'MALE',   'MARRIED', '+254 722 345 109', 'Operations Manager',         'PERMANENT',  185000.00, DATE '2013-10-07', 'ACTIVE',     'HQ-001', 'Operations',              'Standard Chartered', 'Lucy Wanjiru'),
          ('Mercy Atieno',      'EMP-00421', DATE '1997-02-20', 'FEMALE', 'SINGLE',  '+254 722 345 110', 'Operations Assistant',       'CASUAL',      32000.00, DATE '2024-07-01', 'ACTIVE',     'HQ-001', 'Operations',              'Equity Bank',        'Grace Atieno'),
          -- ── Nairobi CBD ──────────────────────────────────────────
          ('Ali Hassan',        'EMP-00422', DATE '1986-06-15', 'MALE',   'MARRIED', '+254 722 345 111', 'Sales Manager',              'PERMANENT',  165000.00, DATE '2016-11-21', 'ACTIVE',     'NBO-002', 'Sales & Marketing',      'KCB Bank',           'Fatuma Hassan'),
          ('Jane Otieno',       'EMP-00423', DATE '1991-10-03', 'FEMALE', 'MARRIED', '+254 722 345 112', 'Sales Executive',            'PERMANENT',   85000.00, DATE '2019-02-04', 'ACTIVE',     'NBO-002', 'Sales & Marketing',      'NCBA Bank',          'Michael Otieno'),
          ('Kevin Mutua',       'EMP-00424', DATE '1995-07-28', 'MALE',   'SINGLE',  '+254 722 345 113', 'Sales Executive',            'CONTRACT',    72000.00, DATE '2023-04-17', 'ACTIVE',     'NBO-002', 'Sales & Marketing',      'Equity Bank',        'Rose Mutua'),
          ('Eunice Wanjiku',    'EMP-00425', DATE '1989-03-09', 'FEMALE', 'MARRIED', '+254 722 345 114', 'Customer Service Manager',   'PERMANENT',  120000.00, DATE '2017-05-22', 'ACTIVE',     'NBO-002', 'Customer Service',       'Co-operative Bank',  'Patrick Njoroge'),
          ('Collins Barasa',    'EMP-00426', DATE '1998-11-16', 'MALE',   'SINGLE',  '+254 722 345 115', 'Customer Service Agent',     'OUTSOURCED',  35000.00, DATE '2024-09-02', 'ACTIVE',     'NBO-002', 'Customer Service',       'KCB Bank',           'Irene Barasa'),
          ('Winnie Achieng',    'EMP-00427', DATE '1994-01-24', 'FEMALE', 'SINGLE',  '+254 722 345 116', 'Customer Service Agent',     'CONTRACT',    48000.00, DATE '2022-10-11', 'INACTIVE',   'NBO-002', 'Customer Service',       'Absa Bank',          'Peter Odhiambo'),
          ('Victor Kilonzo',    'EMP-00428', DATE '1987-09-06', 'MALE',   'MARRIED', '+254 722 345 117', 'Key Account Manager',        'PERMANENT',   98000.00, DATE '2018-01-15', 'ACTIVE',     'NBO-002', 'Sales & Marketing',      'Standard Chartered', 'Agnes Kilonzo'),
          -- ── Mombasa Port ─────────────────────────────────────────
          ('Hamisi Juma',       'EMP-00429', DATE '1984-04-11', 'MALE',   'MARRIED', '+254 722 345 118', 'Warehouse Manager',          'PERMANENT',  140000.00, DATE '2015-07-06', 'ACTIVE',     'MSA-003', 'Warehouse',              'KCB Bank',           'Zainab Juma'),
          ('Fatuma Said',       'EMP-00430', DATE '1992-08-25', 'FEMALE', 'MARRIED', '+254 722 345 119', 'Warehouse Supervisor',       'PERMANENT',   78000.00, DATE '2019-11-18', 'ACTIVE',     'MSA-003', 'Warehouse',              'Equity Bank',        'Omar Said'),
          ('Joseph Mutiso',     'EMP-00431', DATE '1990-02-07', 'MALE',   'MARRIED', '+254 722 345 120', 'Store Keeper',               'PERMANENT',   52000.00, DATE '2018-03-05', 'ACTIVE',     'MSA-003', 'Warehouse',              'Co-operative Bank',  'Beatrice Mutiso'),
          ('Salim Mwinyi',      'EMP-00432', DATE '1988-06-30', 'MALE',   'MARRIED', '+254 722 345 121', 'Clearing Agent',             'PERMANENT',   88000.00, DATE '2017-09-25', 'ACTIVE',     'MSA-003', 'Clearing & Forwarding',  'NCBA Bank',          'Halima Mwinyi'),
          ('Zainab Athman',     'EMP-00433', DATE '1995-12-14', 'FEMALE', 'SINGLE',  '+254 722 345 122', 'Customs Clerk',              'CONTRACT',    58000.00, DATE '2022-06-13', 'ACTIVE',     'MSA-003', 'Clearing & Forwarding',  'Absa Bank',          'Yusuf Athman'),
          ('Patrick Mbithi',    'EMP-00434', DATE '1999-05-02', 'MALE',   'SINGLE',  '+254 722 345 123', 'Loader',                     'OUTSOURCED',  28000.00, DATE '2025-01-13', 'ACTIVE',     'MSA-003', 'Warehouse',              'KCB Bank',           'Caroline Mbithi'),
          -- ── Kisumu Industrial ────────────────────────────────────
          ('Wycliffe Omollo',   'EMP-00435', DATE '1982-10-19', 'MALE',   'MARRIED', '+254 722 345 124', 'Production Manager',         'PERMANENT',  175000.00, DATE '2014-05-12', 'ACTIVE',     'KSM-004', 'Production',             'KCB Bank',           'Judith Omollo'),
          ('Millicent Anyango', 'EMP-00436', DATE '1991-07-08', 'FEMALE', 'MARRIED', '+254 722 345 125', 'Production Supervisor',      'PERMANENT',   82000.00, DATE '2019-08-19', 'ACTIVE',     'KSM-004', 'Production',             'Equity Bank',        'George Ouma'),
          ('Erick Ouma',        'EMP-00437', DATE '1993-03-26', 'MALE',   'SINGLE',  '+254 722 345 126', 'Machine Operator',           'PERMANENT',   45000.00, DATE '2021-02-15', 'ACTIVE',     'KSM-004', 'Production',             'Co-operative Bank',  'Nancy Auma'),
          ('Beatrice Adhiambo', 'EMP-00438', DATE '1996-11-11', 'FEMALE', 'SINGLE',  '+254 722 345 127', 'Quality Assurance Officer',  'CONTRACT',    68000.00, DATE '2023-03-06', 'ACTIVE',     'KSM-004', 'Quality Assurance',      'NCBA Bank',          'Charles Owino'),
          ('Tobias Onyango',    'EMP-00439', DATE '1985-01-17', 'MALE',   'MARRIED', '+254 722 345 128', 'QA Manager',                 'PERMANENT',  128000.00, DATE '2016-08-08', 'ACTIVE',     'KSM-004', 'Quality Assurance',      'Absa Bank',          'Everlyne Akinyi'),
          ('Sharon Akinyi',     'EMP-00440', DATE '2000-09-21', 'FEMALE', 'SINGLE',  '+254 722 345 129', 'Lab Assistant',              'OUTSOURCED',  30000.00, DATE '2025-04-07', 'ACTIVE',     'KSM-004', 'Quality Assurance',      'KCB Bank',           'Brenda Akinyi'),
          -- ── Nakuru ───────────────────────────────────────────────
          ('Moses Kariuki',     'EMP-00441', DATE '1980-05-13', 'MALE',   'MARRIED', '+254 722 345 130', 'Regional Manager',           'PERMANENT',  195000.00, DATE '2013-06-03', 'ACTIVE',     'NKR-005', 'Agriculture Operations',  'Standard Chartered', 'Tabitha Kariuki'),
          ('Lucy Wanjiru',      'EMP-00442', DATE '1990-12-29', 'FEMALE', 'MARRIED', '+254 722 345 131', 'Agronomist',                 'PERMANENT',  112000.00, DATE '2018-07-16', 'ACTIVE',     'NKR-005', 'Agriculture Operations',  'Equity Bank',        'Simon Ngugi'),
          ('Isaac Cheruiyot',   'EMP-00443', DATE '1994-08-04', 'MALE',   'SINGLE',  '+254 722 345 132', 'Field Officer',              'CONTRACT',    62000.00, DATE '2022-02-21', 'ACTIVE',     'NKR-005', 'Agriculture Operations',  'KCB Bank',           'Mercy Chelimo'),
          ('Rebecca Nyambura',  'EMP-00444', DATE '1987-02-12', 'FEMALE', 'MARRIED', '+254 722 345 133', 'Farm Supervisor',            'PERMANENT',   66000.00, DATE '2017-12-04', 'INACTIVE',   'NKR-005', 'Agriculture Operations',  'Co-operative Bank',  'James Karanja'),
          -- ── Thika Road ───────────────────────────────────────────
          ('Anthony Gitau',     'EMP-00445', DATE '1983-09-17', 'MALE',   'MARRIED', '+254 722 345 134', 'Retail Manager',             'PERMANENT',  158000.00, DATE '2015-03-09', 'ACTIVE',     'THK-006', 'Retail Operations',      'KCB Bank',           'Miriam Gitau'),
          ('Nancy Wairimu',     'EMP-00446', DATE '1995-06-23', 'FEMALE', 'SINGLE',  '+254 722 345 135', 'Retail Supervisor',          'PERMANENT',   72000.00, DATE '2020-11-02', 'ACTIVE',     'THK-006', 'Retail Operations',      'Equity Bank',        'Paul Mburu'),
          ('Dennis Kimani',     'EMP-00447', DATE '1997-04-06', 'MALE',   'SINGLE',  '+254 722 345 136', 'Cashier',                    'CONTRACT',    42000.00, DATE '2023-08-14', 'ACTIVE',     'THK-006', 'Retail Operations',      'Absa Bank',          'Lydia Kimani'),
          ('Purity Wanjala',    'EMP-00448', DATE '1992-10-08', 'FEMALE', 'MARRIED', '+254 722 345 137', 'Merchandiser',               'OUTSOURCED',  31000.00, DATE '2024-11-25', 'ACTIVE',     'THK-006', 'Retail Operations',      'NCBA Bank',          'Moses Wanjala'),
          -- ── Eldoret Depot ────────────────────────────────────────
          ('Elijah Tanui',      'EMP-00449', DATE '1986-01-23', 'MALE',   'MARRIED', '+254 722 345 138', 'Logistics Manager',          'PERMANENT',  148000.00, DATE '2016-09-19', 'ACTIVE',     'ELD-007', 'Logistics',              'Standard Chartered', 'Susan Tanui'),
          ('Gladys Jepkosgei',  'EMP-00450', DATE '1993-07-31', 'FEMALE', 'MARRIED', '+254 722 345 139', 'Dispatch Officer',           'PERMANENT',   58000.00, DATE '2021-05-10', 'ACTIVE',     'ELD-007', 'Logistics',              'KCB Bank',           'Wesley Rono'),
          ('Felix Sang',        'EMP-00451', DATE '1990-11-27', 'MALE',   'SINGLE',  '+254 722 345 140', 'Driver',                     'PERMANENT',   46000.00, DATE '2019-04-08', 'TERMINATED', 'ELD-007', 'Logistics',              'Equity Bank',        'Irene Sang')
     ) AS v(full_name, employee_number, date_of_birth, gender, marital_status, phone, position,
            employment_type, basic_salary, date_hired, employment_status, branch_code, dept_name,
            bank_name, emergency_contact_name)
         JOIN branches b ON b.code = v.branch_code
         LEFT JOIN departments d ON d.name = v.dept_name AND d.branch_id = b.id
         CROSS JOIN LATERAL (SELECT substring(v.employee_number FROM 5)::int AS seq) n
    ON CONFLICT (employee_number) DO NOTHING;

-- ------------------------------------------------------------
-- 7. Employee documents — three per employee so the Documents
--    tab and the document widgets are not empty.
--    `document_type` vocabulary is from EmployeeDocument's
--    comment: CONTRACT, ID, CERTIFICATE, OTHER.
--    `file_url` is NOT NULL; these point at the storage prefix
--    the backend hands out.
-- ------------------------------------------------------------
INSERT INTO employee_documents (employee_id, document_type, file_url, file_name, created_at, updated_at)
SELECT e.id, v.document_type,
       '/uploads/employees/' || e.employee_number || '/' || lower(v.document_type) || '.pdf',
       v.document_type || '.pdf',
       now(), now()
FROM employees e
         CROSS JOIN (VALUES ('ID'), ('CONTRACT'), ('CERTIFICATE')) AS v(document_type)
WHERE NOT EXISTS (
    SELECT 1 FROM employee_documents ed
    WHERE ed.employee_id = e.id AND ed.document_type = v.document_type
);

-- ------------------------------------------------------------
-- 8. Users
--
-- 8a. SUPER_ADMIN. Reuses the SAME bcrypt hash as the six staff
--     accounts seeded in V4, so the dev password that already
--     works for ADMIN works here too — no new secret.
--     `must_change_password = true` is the documented first-login
--     behaviour (README §Running): proxy.ts and dal.ts pin the
--     session to /set-password until a new password is chosen.
-- 8b. BRANCH-MANAGER is linked to Head Office. Without this,
--     `users.branch_id` is NULL and AccessGuard.currentBranchId()
--     throws, which made the whole /branch portal unreachable.
-- ------------------------------------------------------------
INSERT INTO users (username, password, role, display_name, branch_id, active, must_change_password, created_at, updated_at)
SELECT 'SUPER-ADMIN',
       '$2b$10$ogv/i91wk.CwKv.4WEPAtuO9bEAnz.cx42V9xGBRuWmlh6EU0XMbO',
       'SUPER_ADMIN',
       'Super Administrator',
       NULL, true, true, now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'SUPER-ADMIN');

UPDATE users
SET branch_id = (SELECT id FROM branches WHERE code = 'HQ-001'),
    updated_at = now()
WHERE username = 'BRANCH-MANAGER'
  AND branch_id IS NULL;

-- ------------------------------------------------------------
-- 9. Leave types. `name` is the label shown in the portal and is
--    looked up by name (LeaveTypeRepository.findByName), so these
--    match the vocabulary in LeaveType's comment: ANNUAL, SICK,
--    MATERNITY, PATERNITY, COMPASSIONATE, UNPAID.
-- ------------------------------------------------------------
INSERT INTO leave_types (name, default_entitlement_days, paid, created_at, updated_at)
VALUES
    ('ANNUAL',        21, true,  now(), now()),
    ('SICK',          14, true,  now(), now()),
    ('MATERNITY',     90, true,  now(), now()),
    ('PATERNITY',     14, true,  now(), now()),
    ('COMPASSIONATE',  7, true,  now(), now()),
    ('UNPAID',         0, false, now(), now())
    ON CONFLICT (name) DO NOTHING;

-- ------------------------------------------------------------
-- 10. Leave requests.
--
-- Dates are relative to today, and exactly two employees are on
-- approved leave TODAY. `BiometricSyncServiceImpl` refuses to
-- record attendance for anyone on approved leave, and the
-- attendance block below skips them — so these rows have to exist
-- first, or the attendance numbers and the "On Leave Today"
-- widget would disagree with each other.
--
-- The approver ids are scalar subqueries rather than joins: a
-- join onto a missing username would silently drop the row,
-- whereas a scalar subquery just yields NULL.
-- ------------------------------------------------------------
INSERT INTO leave_requests (
    employee_id, leave_type_id, start_date, end_date, reason, status,
    approved_by_manager_id, approved_by_hr_id, rejection_reason,
    created_at, updated_at
)
SELECT e.id, lt.id, v.start_offset, v.end_offset, v.reason, v.status,
       CASE WHEN v.status IN ('APPROVED', 'PENDING_HR')
                THEN (SELECT id FROM users WHERE username = 'BRANCH-MANAGER') END,
       CASE WHEN v.status = 'APPROVED'
                THEN (SELECT id FROM users WHERE username = 'HR-MANAGER') END,
       v.rejection_reason,
       now(), now()
FROM (VALUES
          -- On leave today — drives the "On Leave Today" widget.
          ('EMP-00442', 'ANNUAL',        CURRENT_DATE - 3,  CURRENT_DATE + 7,  'Annual family leave',        'APPROVED',        NULL),
          ('EMP-00447', 'SICK',          CURRENT_DATE - 1,  CURRENT_DATE + 2,  'Recovering from malaria',    'APPROVED',        NULL),
          -- Historical, already taken.
          ('EMP-00412', 'ANNUAL',        CURRENT_DATE - 62, CURRENT_DATE - 53, 'Annual leave',               'APPROVED',        NULL),
          ('EMP-00413', 'SICK',          CURRENT_DATE - 41, CURRENT_DATE - 39, 'Flu',                        'APPROVED',        NULL),
          ('EMP-00422', 'ANNUAL',        CURRENT_DATE - 90, CURRENT_DATE - 81, 'Annual leave',               'APPROVED',        NULL),
          ('EMP-00435', 'COMPASSIONATE', CURRENT_DATE - 70, CURRENT_DATE - 67, 'Bereavement',                'APPROVED',        NULL),
          -- Awaiting the branch manager.
          ('EMP-00416', 'ANNUAL',        CURRENT_DATE + 14, CURRENT_DATE + 21, 'Wedding',                    'PENDING_MANAGER', NULL),
          ('EMP-00423', 'ANNUAL',        CURRENT_DATE + 10, CURRENT_DATE + 17, 'Annual leave',               'PENDING_MANAGER', NULL),
          ('EMP-00431', 'COMPASSIONATE', CURRENT_DATE + 5,  CURRENT_DATE + 9,  'Family emergency',           'PENDING_MANAGER', NULL),
          ('EMP-00445', 'ANNUAL',        CURRENT_DATE + 21, CURRENT_DATE + 28, 'Annual leave',               'PENDING_MANAGER', NULL),
          -- Manager approved, awaiting HR.
          ('EMP-00418', 'ANNUAL',        CURRENT_DATE + 7,  CURRENT_DATE + 14, 'Annual leave',               'PENDING_HR',      NULL),
          ('EMP-00429', 'PATERNITY',     CURRENT_DATE + 3,  CURRENT_DATE + 16, 'Newborn',                    'PENDING_HR',      NULL),
          ('EMP-00439', 'UNPAID',        CURRENT_DATE + 30, CURRENT_DATE + 44, 'Professional certification', 'PENDING_HR',      NULL),
          -- Rejected.
          ('EMP-00428', 'ANNUAL',        CURRENT_DATE + 2,  CURRENT_DATE + 12, 'Annual leave',               'REJECTED',        'Peak sales period — resubmit after month end'),
          ('EMP-00437', 'UNPAID',        CURRENT_DATE + 1,  CURRENT_DATE + 30, 'Personal travel',            'REJECTED',        'Unpaid leave capped at 7 days')
     ) AS v(employee_number, leave_type_name, start_offset, end_offset, reason, status, rejection_reason)
         JOIN employees e ON e.employee_number = v.employee_number
         JOIN leave_types lt ON lt.name = v.leave_type_name
WHERE NOT EXISTS (
    SELECT 1 FROM leave_requests lr
    WHERE lr.employee_id = e.id AND lr.start_date = v.start_offset
);

-- ------------------------------------------------------------
-- 11. Shifts + assignments.
--     `type` must match the ShiftType enum:
--     MORNING, EVENING, NIGHT, ROTATING, FLEXIBLE.
--     Neither table has a unique key on these columns, so both
--     use NOT EXISTS.
-- ------------------------------------------------------------
INSERT INTO shifts (name, type, start_time, end_time, created_at, updated_at)
SELECT v.name, v.type, v.start_time, v.end_time, now(), now()
FROM (VALUES
          ('Day Shift',      'MORNING',  TIME '08:00', TIME '17:00'),
          ('Evening Shift',  'EVENING',  TIME '14:00', TIME '22:00'),
          ('Night Shift',    'NIGHT',    TIME '20:00', TIME '05:00'),
          ('Rotating Shift', 'ROTATING', TIME '08:00', TIME '20:00'),
          ('Flexible Shift', 'FLEXIBLE', NULL::time,    NULL::time)
     ) AS v(name, type, start_time, end_time)
WHERE NOT EXISTS (SELECT 1 FROM shifts s WHERE s.name = v.name);

-- Department-level roster: one shift covers a whole department.
INSERT INTO shift_assignments (shift_id, department_id, created_at, updated_at)
SELECT s.id, d.id, now(), now()
FROM (VALUES
          ('Day Shift',      'Operations',         'HQ-001'),
          ('Day Shift',      'Sales & Marketing',  'NBO-002'),
          ('Night Shift',    'Warehouse',          'MSA-003'),
          ('Day Shift',      'Production',         'KSM-004'),
          ('Rotating Shift', 'Quality Assurance',  'KSM-004'),
          ('Day Shift',      'Retail Operations',  'THK-006')
     ) AS v(shift_name, dept_name, branch_code)
         JOIN shifts s ON s.name = v.shift_name
         JOIN branches b ON b.code = v.branch_code
         JOIN departments d ON d.name = v.dept_name AND d.branch_id = b.id
WHERE NOT EXISTS (
    SELECT 1 FROM shift_assignments sa WHERE sa.shift_id = s.id AND sa.department_id = d.id
);

-- Employee-level roster for the night crew, who are not tied to a
-- single department.
INSERT INTO shift_assignments (shift_id, employee_id, branch_id, created_at, updated_at)
SELECT s.id, e.id, e.branch_id, now(), now()
FROM (VALUES
          ('EMP-00431', 'Night Shift'),
          ('EMP-00432', 'Night Shift'),
          ('EMP-00434', 'Night Shift'),
          ('EMP-00449', 'Rotating Shift'),
          ('EMP-00450', 'Rotating Shift')
     ) AS v(employee_number, shift_name)
         JOIN employees e ON e.employee_number = v.employee_number
         JOIN shifts s ON s.name = v.shift_name
WHERE NOT EXISTS (
    SELECT 1 FROM shift_assignments sa WHERE sa.shift_id = s.id AND sa.employee_id = e.id
);

-- ------------------------------------------------------------
-- 12. Biometric terminals — one per branch.
--
--     Serials are namespaced and deliberately do NOT include
--     `SECUGEN-BRANCH1-003`, which is the terminal HrpayrollDesktop
--     is enrolled against. `device_serial` is UNIQUE, so
--     ON CONFLICT protects a pre-existing row too.
--
--     `last_sync_at` is left NULL on purpose. `isOnline()` is a
--     5-minute window, so writing `now()` here would show every
--     terminal green for five minutes after the migration and red
--     forever after — a number that means nothing. NULL reads as
--     OFFLINE, which is true: these terminals are not running.
-- ------------------------------------------------------------
INSERT INTO biometric_devices (branch_id, device_serial, device_token_hash, last_sync_at, status, created_at, updated_at)
SELECT b.id,
       'DHA-' || b.code || '-001',
       encode(sha256(('dha-device-' || b.code)::bytea), 'hex'),
       NULL, 'ACTIVE', now(), now()
FROM branches b
WHERE b.code IN ('HQ-001', 'NBO-002', 'MSA-003', 'KSM-004', 'NKR-005', 'THK-006', 'ELD-007')
    ON CONFLICT (device_serial) DO NOTHING;

-- ------------------------------------------------------------
-- 13. Attendance — 30 days, Mondays through Saturdays.
--
--     The status mix is deterministic (a hash of employee id and
--     day offset) rather than random, so the same database always
--     produces the same history and the dashboard does not
--     reshuffle on every restart.
--
--     Guards:
--     · ACTIVE employees only — a terminated employee accruing
--       new attendance would be nonsense.
--     · No rows before `date_hired`.
--     · No rows on an approved leave day (the engine refuses to
--       record those, so seeding them would contradict it).
--     · Not today for EMP-1001 — see the header note.
--
--     Today's rows are left with a NULL `clock_out` for roughly
--     two thirds of staff, which is what makes "On Site Now"
--     non-zero on the admin board.
-- ------------------------------------------------------------
WITH today AS (
    SELECT (now() AT TIME ZONE 'Africa/Nairobi')::date AS d
),
     grid AS (
         SELECT
             e,
             o.off,
             t.d - o.off AS the_day,
             ((e.id * 37 + o.off * 17) % 100) AS bucket
FROM employees e
    CROSS JOIN today t
    CROSS JOIN generate_series(0, 29) AS o(off)
WHERE e.employment_status = 'ACTIVE'
  AND (t.d - o.off) >= e.date_hired
  AND EXTRACT(ISODOW FROM (t.d - o.off)) <= 6          -- Mon–Sat
  AND NOT (o.off = 0 AND e.employee_number = 'EMP-1001')
  AND NOT EXISTS (
    SELECT 1 FROM leave_requests lr
    WHERE lr.employee_id = e.id
  AND lr.status = 'APPROVED'
  AND (t.d - o.off) BETWEEN lr.start_date AND lr.end_date
    )
    ),
    classified AS (
SELECT
    g.*,
    CASE
    WHEN g.bucket < 76 THEN 'PRESENT'
    WHEN g.bucket < 87 THEN 'LATE'
    WHEN g.bucket < 95 THEN 'ABSENT'
    ELSE 'OVERTIME'
    END AS att_status
FROM grid g
    )
INSERT INTO attendance (
    employee_id, branch_id, attendance_date, clock_in, clock_out,
    source, status, event_id, clock_out_event_id, device_id, verify_method,
    created_at, updated_at
)
SELECT
    c.e.id,
    c.e.branch_id,
    c.the_day,
    -- Clock-in: PRESENT 07:35–08:04, LATE 08:25–09:29, OVERTIME 07:50–08:04.
    CASE c.att_status
        WHEN 'ABSENT'   THEN NULL
        WHEN 'LATE'     THEN c.the_day + TIME '08:25' + ((c.bucket % 65) || ' minutes')::interval
        WHEN 'OVERTIME' THEN c.the_day + TIME '07:50' + ((c.bucket % 15) || ' minutes')::interval
        ELSE                 c.the_day + TIME '07:35' + ((c.bucket % 30) || ' minutes')::interval
END,
    -- Clock-out: NULL on an ABSENT day, and NULL today for anyone
    -- who has not scanned out yet.
    CASE
        WHEN c.att_status = 'ABSENT' THEN NULL
        WHEN c.off = 0 AND c.e.id % 3 <> 0 THEN NULL
        WHEN c.att_status = 'OVERTIME'
            THEN c.the_day + TIME '19:30' + ((c.bucket % 60) || ' minutes')::interval
        ELSE     c.the_day + TIME '17:00' + ((c.bucket % 45) || ' minutes')::interval
END,
    CASE WHEN c.bucket % 10 < 8 THEN 'BIOMETRIC' ELSE 'MANUAL' END,
    c.att_status,
    'EVT-'     || c.e.employee_number || '-' || to_char(c.the_day, 'YYYYMMDD'),
    CASE
        WHEN c.att_status = 'ABSENT' THEN NULL
        WHEN c.off = 0 AND c.e.id % 3 <> 0 THEN NULL
        ELSE 'EVT-OUT-' || c.e.employee_number || '-' || to_char(c.the_day, 'YYYYMMDD')
END,
    dev.id,
    CASE WHEN c.bucket % 10 < 8 THEN 'FINGERPRINT' ELSE NULL END,
    now(), now()
FROM classified c
LEFT JOIN LATERAL (
    SELECT d.id FROM biometric_devices d
    WHERE d.branch_id = c.e.branch_id
    ORDER BY d.id
    LIMIT 1
) dev ON true
ON CONFLICT (event_id) DO NOTHING;

-- ------------------------------------------------------------
-- 14. Loans — seeded BEFORE payslips, because the payroll engine
--     deducts `min(installment_amount, outstanding_balance)` for
--     every loan in status 'APPROVED'. Seeding them after would
--     produce payslips whose loan_deduction does not match the
--     loans on screen.
--
--     `outstanding_balance` starts at the FULL principal. The
--     repayment block (section 17) then posts one instalment and
--     subtracts exactly that, which is what keeps the invariant
--     `outstanding_balance = principal - SUM(repayments)` true.
--     Seeding `principal - installment` here instead would make
--     the repayment subtract twice and leave the balance wrong by
--     one instalment for every loan on the books.
-- ------------------------------------------------------------
INSERT INTO loans (
    employee_id, principal_amount, interest_rate, installment_amount,
    outstanding_balance, request_date, approval_date, status,
    created_at, updated_at
)
SELECT e.id, v.principal, v.rate, v.installment,
       v.principal,
       v.request_date, v.approval_date, v.status,
       now(), now()
FROM (VALUES
          ('EMP-00414', 600000.00, 8.00, 25000.00, CURRENT_DATE - 210, CURRENT_DATE - 205, 'APPROVED'),
          ('EMP-00422', 350000.00, 10.00, 14583.33, CURRENT_DATE - 160, CURRENT_DATE - 155, 'APPROVED'),
          ('EMP-00435', 500000.00, 8.00, 20833.33, CURRENT_DATE - 120, CURRENT_DATE - 117, 'APPROVED'),
          ('EMP-00445', 250000.00, 9.00, 10416.67, CURRENT_DATE - 95, CURRENT_DATE - 92, 'APPROVED'),
          ('EMP-00412', 180000.00, 8.00, 7500.00, CURRENT_DATE - 80, CURRENT_DATE - 76, 'APPROVED'),
          ('EMP-00420', 900000.00, 7.50, 37500.00, CURRENT_DATE - 45, CURRENT_DATE - 41, 'APPROVED'),
          ('EMP-00425', 120000.00, 8.00, 5000.00, CURRENT_DATE - 9, NULL, 'PENDING'),
          ('EMP-00416', 300000.00, 9.00, 12500.00, CURRENT_DATE - 6, NULL, 'PENDING'),
          ('EMP-00431', 80000.00, 8.00, 3333.33, CURRENT_DATE - 20, CURRENT_DATE - 16, 'REJECTED'),
          ('EMP-00429', 150000.00, 8.00, 6250.00, CURRENT_DATE - 400, CURRENT_DATE - 390, 'CLOSED')
     ) AS v(employee_number, principal, rate, installment, request_date, approval_date, status)
         JOIN employees e ON e.employee_number = v.employee_number
WHERE NOT EXISTS (
    SELECT 1 FROM loans l
    WHERE l.employee_id = e.id AND l.principal_amount = v.principal
);

-- A CLOSED loan is fully repaid by definition.
UPDATE loans
SET outstanding_balance = 0.00, updated_at = now()
WHERE status = 'CLOSED' AND outstanding_balance <> 0.00;

-- ------------------------------------------------------------
-- 15. Payroll runs — one COMPLETED for last month, one
--     IN_PROGRESS for this month. `status` is the
--     PayrollRunStatus enum, so only IN_PROGRESS / COMPLETED /
--     FAILED are valid. (No 'DRAFT' — that value does not exist
--     in the enum, which is exactly the trap V6 had to patch.)
--     PayrollLockService only guards a live run with an advisory
--     lock — it does not lock a period — so an IN_PROGRESS row
--     here cannot block attendance writes.
-- ------------------------------------------------------------
INSERT INTO payroll_runs (
    period_start, period_end, scope_branch_ids, scope_agent_ids, scope_department_ids,
    status, total_gross_payroll, total_net_payroll, initiated_by_user_id,
    created_at, updated_at
)
SELECT
    (date_trunc('month', (now() AT TIME ZONE 'Africa/Nairobi')::date) - INTERVAL '1 month')::date,
    (date_trunc('month', (now() AT TIME ZONE 'Africa/Nairobi')::date) - INTERVAL '1 day')::date,
    NULL, NULL, NULL,
    'COMPLETED', NULL, NULL,
    (SELECT id FROM users WHERE username = 'PAYROLL-OFFICER'),
    now(), now()
    WHERE NOT EXISTS (
    SELECT 1 FROM payroll_runs
    WHERE status = 'COMPLETED'
      AND period_start = (date_trunc('month', (now() AT TIME ZONE 'Africa/Nairobi')::date) - INTERVAL '1 month')::date
);

INSERT INTO payroll_runs (
    period_start, period_end, scope_branch_ids, scope_agent_ids, scope_department_ids,
    status, total_gross_payroll, total_net_payroll, initiated_by_user_id,
    created_at, updated_at
)
SELECT
    date_trunc('month', (now() AT TIME ZONE 'Africa/Nairobi')::date)::date,
    (date_trunc('month', (now() AT TIME ZONE 'Africa/Nairobi')::date) + INTERVAL '1 month' - INTERVAL '1 day')::date,
    NULL, NULL, NULL,
    'IN_PROGRESS', NULL, NULL,
    (SELECT id FROM users WHERE username = 'PAYROLL-OFFICER'),
    now(), now()
    WHERE NOT EXISTS (
    SELECT 1 FROM payroll_runs
    WHERE status = 'IN_PROGRESS'
      AND period_start = date_trunc('month', (now() AT TIME ZONE 'Africa/Nairobi')::date)::date
);

-- ------------------------------------------------------------
-- 16. Payslips for the COMPLETED run.
--
--     Every formula below is lifted from the engine, so a real
--     re-run produces the same figures:
--       SHIF     = max(gross × 2.75%, 300)
--       NSSF     = min(gross, 8000) × 6%
--       Housing  = gross × 1.5%
--       NITA     = 50 flat
--       PAYE     = banded on (gross − NSSF), less 2,400 relief
--       Overtime = (basic / (8 × working days)) × hours × 1.5
--       Loan     = min(installment, outstanding)
--
--     `working_days` counts Mon–Fri, because CompanySettings
--     declares SATURDAY and SUNDAY as the weekend. The attendance
--     grid above runs Mon–Sat, so a Saturday OVERTIME row does
--     feed hours into this formula that the working-day divisor
--     does not account for. That inflates seeded overtime slightly;
--     it does not affect any other figure.
--
--     Allowances are zero because `PayrollCalculationServiceImpl`
--     hard-codes them to zero today — a seeded 15% house
--     allowance would be a figure the system cannot produce.
-- ------------------------------------------------------------
WITH run AS (
    SELECT id, period_start, period_end
    FROM payroll_runs
    WHERE status = 'COMPLETED'
    ORDER BY period_start DESC
    LIMIT 1
    ),
    working_days AS (
SELECT count(*) AS days
FROM run r
    CROSS JOIN LATERAL generate_series(r.period_start, r.period_end, INTERVAL '1 day') g(d)
WHERE EXTRACT(ISODOW FROM g.d) < 6
    ),
    overtime AS (
SELECT a.employee_id,
    SUM(EXTRACT(EPOCH FROM (a.clock_out - a.clock_in)) / 3600.0) AS hours
FROM attendance a
    JOIN run r ON a.attendance_date BETWEEN r.period_start AND r.period_end
WHERE a.status = 'OVERTIME'
  AND a.clock_in IS NOT NULL
  AND a.clock_out IS NOT NULL
GROUP BY a.employee_id
    ),
    loan_deduction AS (
SELECT l.employee_id,
    SUM(LEAST(l.installment_amount, l.outstanding_balance)) AS amount
FROM loans l
WHERE l.status = 'APPROVED'
GROUP BY l.employee_id
    ),
    figures AS (
SELECT
    e.id AS employee_id,
    e.basic_salary,
    ROUND(
    COALESCE(ot.hours, 0)::numeric
    * (e.basic_salary / (8 * wd.days))
    * 1.5,
    2
    ) AS overtime_pay,
    COALESCE(ld.amount, 0.00) AS loan_deduction
FROM employees e
    CROSS JOIN working_days wd
    LEFT JOIN overtime ot ON ot.employee_id = e.id
    LEFT JOIN loan_deduction ld ON ld.employee_id = e.id
WHERE e.employment_status = 'ACTIVE'
    ),
    grossed AS (
SELECT f.*, f.basic_salary + f.overtime_pay AS gross_pay
FROM figures f
    ),
    levied AS (
SELECT
    g.*,
    GREATEST(ROUND(g.gross_pay * 0.0275, 2), 300.00) AS shif,
    ROUND(LEAST(g.gross_pay, 8000.00) * 0.06, 2)     AS nssf,
    ROUND(g.gross_pay * 0.015, 2)                    AS housing_levy,
    50.00                                            AS nita
FROM grossed g
    ),
    taxed AS (
SELECT
    l.*,
    GREATEST(
    (
    LEAST(GREATEST(l.gross_pay - l.nssf, 0), 24000.00) * 0.10
    + LEAST(GREATEST(l.gross_pay - l.nssf - 24000.00, 0), 8333.00) * 0.25
    + LEAST(GREATEST(l.gross_pay - l.nssf - 32333.00, 0), 467667.00) * 0.30
    + LEAST(GREATEST(l.gross_pay - l.nssf - 500000.00, 0), 300000.00) * 0.325
    + GREATEST(l.gross_pay - l.nssf - 800000.00, 0) * 0.35
    ) - 2400.00,
    0
    ) AS paye
FROM levied l
    )
INSERT INTO payslips (
    payroll_run_id, employee_id, basic_salary,
    house_allowance, transport_allowance, overtime_pay, bonus, other_allowances,
    gross_pay, shif_deduction, nssf_deduction, housing_levy_deduction,
    paye_deduction, nita_deduction, loan_deduction, other_deductions, net_pay,
    created_at, updated_at
)
SELECT
    r.id,
    t.employee_id,
    t.basic_salary,
    0.00, 0.00, t.overtime_pay, 0.00, 0.00,
    t.gross_pay,
    t.shif, t.nssf, t.housing_levy,
    ROUND(t.paye, 2), t.nita, t.loan_deduction, 0.00,
    ROUND(
            t.gross_pay
                - t.shif - t.nssf - t.housing_levy - ROUND(t.paye, 2) - t.nita - t.loan_deduction,
            2
    ),
    now(), now()
FROM taxed t
         CROSS JOIN run r
WHERE NOT EXISTS (
    SELECT 1 FROM payslips p WHERE p.payroll_run_id = r.id AND p.employee_id = t.employee_id
);

-- ------------------------------------------------------------
-- 17. Loan repayments for the COMPLETED run, then roll the
--     balances forward.
--
--     Each employee here holds at most one APPROVED loan, so the
--     join from loans to payslips on employee_id yields one
--     repayment per loan. If a second APPROVED loan were ever
--     added for the same employee, this join would post the full
--     payslip deduction against EACH loan and double-count — the
--     guard only protects against re-running this migration, not
--     against that.
-- ------------------------------------------------------------
INSERT INTO loan_repayments (loan_id, payroll_run_id, amount_paid, created_at, updated_at)
SELECT l.id, p.payroll_run_id, p.loan_deduction, now(), now()
FROM loans l
         JOIN payslips p ON p.employee_id = l.employee_id
         JOIN payroll_runs r ON r.id = p.payroll_run_id AND r.status = 'COMPLETED'
WHERE l.status = 'APPROVED'
  AND p.loan_deduction > 0
  AND NOT EXISTS (
    SELECT 1 FROM loan_repayments lr
    WHERE lr.loan_id = l.id AND lr.payroll_run_id = p.payroll_run_id
);

-- `outstanding_balance` is now principal minus everything repaid,
-- which is the number the Loans screens display.
UPDATE loans l
SET outstanding_balance = GREATEST(
        l.outstanding_balance - COALESCE((
                                             SELECT SUM(lr.amount_paid) FROM loan_repayments lr WHERE lr.loan_id = l.id
                                         ), 0),
        0
                          ),
    updated_at = now()
WHERE l.status = 'APPROVED';

-- ------------------------------------------------------------
-- 18. Agent advances — outstanding balances are what the invoice
--     formula recovers, capped at the amount payable.
-- ------------------------------------------------------------
INSERT INTO agent_advances (agent_id, amount, advance_date, purpose, outstanding_balance, status, created_at, updated_at)
SELECT a.id, v.amount, v.advance_date, v.purpose, v.outstanding, v.status, now(), now()
FROM (VALUES
          ('Test Agent Ltd',           120000.00, CURRENT_DATE - 75,  'Mobilisation advance for new deployment', 90000.00, 'PARTIALLY_RECOVERED'),
          ('Coastal Manpower Ltd',      85000.00, CURRENT_DATE - 40,  'Advance on October invoice',              85000.00, 'OUTSTANDING'),
          ('Mavuno Staffing Services',  60000.00, CURRENT_DATE - 18,  'Recruitment cost advance',                60000.00, 'OUTSTANDING'),
          ('Test Agent Ltd',            45000.00, CURRENT_DATE - 300, 'Staff transport advance',                     0.00, 'FULLY_RECOVERED')
     ) AS v(agent_name, amount, advance_date, purpose, outstanding, status)
         JOIN agents a ON a.name = v.agent_name
WHERE NOT EXISTS (
    SELECT 1 FROM agent_advances aa
    WHERE aa.agent_id = a.id AND aa.amount = v.amount
);

-- ------------------------------------------------------------
-- 19. Agent payroll summaries for the COMPLETED run — the
--     invoices the agent portal and the finance portal both read.
--     Formula per AgentPayrollCalculationServiceImpl §6:
--       Gross + Service Charge + SHIF + NSSF + Housing + PAYE
--       + Loan Deductions − Advance Recovery = Subtotal
--       Subtotal + 16% VAT (when registered)  = Final Payable
--     Only agents with at least one payslip in the run get a row:
--     an agent with no deployed staff is not owed an invoice.
-- ------------------------------------------------------------
WITH run AS (
    SELECT id FROM payroll_runs WHERE status = 'COMPLETED' ORDER BY period_start DESC LIMIT 1
    )
INSERT INTO agent_payroll_summaries (
    payroll_run_id, agent_id, employee_count,
    gross_salaries, service_charge,
    shif_total, nssf_total, housing_levy_total, paye_total, employee_loan_deductions_total,
    advance_recovery_total, subtotal_before_vat,
    vat_applied, vat_rate_used, vat_amount, final_amount_payable,
    created_at, updated_at
)
SELECT
    r.id,
    a.id,
    agg.employee_count,
    agg.gross_salaries,
    charge.service_charge,
    agg.shif_total, agg.nssf_total, agg.housing_levy_total, agg.paye_total, agg.loan_total,
    recovery.advance_recovery,
    subtotal.subtotal_before_vat,
    a.vat_registered,
    CASE WHEN a.vat_registered THEN 16.00 ELSE 0.00 END,
    CASE WHEN a.vat_registered THEN ROUND(subtotal.subtotal_before_vat * 0.16, 2) ELSE 0.00 END,
    subtotal.subtotal_before_vat
        + CASE WHEN a.vat_registered THEN ROUND(subtotal.subtotal_before_vat * 0.16, 2) ELSE 0.00 END,
    now(), now()
FROM run r
         CROSS JOIN agents a
         JOIN LATERAL (
    SELECT
        count(*)                                   AS employee_count,
        COALESCE(SUM(p.gross_pay), 0)              AS gross_salaries,
        COALESCE(SUM(p.shif_deduction), 0)         AS shif_total,
        COALESCE(SUM(p.nssf_deduction), 0)         AS nssf_total,
        COALESCE(SUM(p.housing_levy_deduction), 0) AS housing_levy_total,
        COALESCE(SUM(p.paye_deduction), 0)         AS paye_total,
        COALESCE(SUM(p.loan_deduction), 0)         AS loan_total
    FROM employees e
             JOIN payslips p ON p.employee_id = e.id AND p.payroll_run_id = r.id
    WHERE e.agent_id = a.id
        ) agg ON agg.employee_count > 0
         CROSS JOIN LATERAL (
    SELECT CASE a.charge_type
               WHEN 'PER_EMPLOYEE'        THEN ROUND(a.charge_amount * agg.employee_count, 2)
               WHEN 'PERCENTAGE_OF_GROSS' THEN ROUND(agg.gross_salaries * a.charge_percentage / 100, 2)
               ELSE ROUND(a.charge_amount, 2)
               END AS service_charge
        ) charge
         CROSS JOIN LATERAL (
    SELECT LEAST(
                   COALESCE((
                                SELECT SUM(aa.outstanding_balance) FROM agent_advances aa
                                WHERE aa.agent_id = a.id
                                  AND aa.status IN ('OUTSTANDING', 'PARTIALLY_RECOVERED')
                            ), 0),
                   agg.gross_salaries + charge.service_charge + agg.shif_total + agg.nssf_total
                       + agg.housing_levy_total + agg.paye_total + agg.loan_total
           ) AS advance_recovery
        ) recovery
         CROSS JOIN LATERAL (
    SELECT agg.gross_salaries + charge.service_charge + agg.shif_total + agg.nssf_total
               + agg.housing_levy_total + agg.paye_total + agg.loan_total
               - recovery.advance_recovery AS subtotal_before_vat
        ) subtotal
WHERE NOT EXISTS (
    SELECT 1 FROM agent_payroll_summaries s
    WHERE s.payroll_run_id = r.id AND s.agent_id = a.id
);

-- Roll the totals back onto the run so the payroll screens do not
-- have to re-aggregate 40 payslips to show two numbers.
UPDATE payroll_runs r
SET total_gross_payroll = agg.gross_total,
    total_net_payroll   = agg.net_total,
    updated_at          = now()
    FROM (
    SELECT payroll_run_id,
           SUM(gross_pay) AS gross_total,
           SUM(net_pay)   AS net_total
    FROM payslips
    GROUP BY payroll_run_id
) agg
WHERE r.id = agg.payroll_run_id
  AND r.total_gross_payroll IS NULL;

-- ------------------------------------------------------------
-- 20. Notifications — so the topbar bell has a real unread count
--     for each staff role. Usernames are exactly those seeded by
--     V4: ADMIN, HR-MANAGER, PAYROLL-OFFICER, BRANCH-MANAGER,
--     FINANCE-OFFICER, AUDITOR, plus AGENT from V5.
--     `type` vocabulary is from Notification's comment;
--     `channel` is EMAIL / SMS / IN_APP. `read` is a real column
--     name (unreserved in Postgres, so it needs no quoting).
-- ------------------------------------------------------------
INSERT INTO notifications (recipient_user_id, channel, type, message, read, created_at, updated_at)
SELECT u.id, v.channel, v.type, v.message, v.is_read,
       now() - (v.age_minutes || ' minutes')::interval, now()
FROM (VALUES
          ('ADMIN',           'IN_APP', 'PAYROLL_COMPLETED', 'September payroll run completed — 40 payslips generated.',      false, 240),
          ('ADMIN',           'IN_APP', 'ATTENDANCE_ALERT',  'Nakuru reported 2 absences without approval today.',            false, 95),
          ('ADMIN',           'EMAIL',  'LEAVE_APPROVED',    'Leave request from Brian Ochieng is awaiting your review.',     true,  2880),
          ('SUPER-ADMIN',     'IN_APP', 'PAYROLL_COMPLETED', 'September payroll run completed and locked.',                   false, 240),
          ('SUPER-ADMIN',     'IN_APP', 'ATTENDANCE_ALERT',  'Mombasa Port terminal has not synced in over 24 hours.',        false, 620),
          ('HR-MANAGER',      'IN_APP', 'LEAVE_APPROVED',    '2 leave requests have moved to HR approval.',                   false, 180),
          ('HR-MANAGER',      'IN_APP', 'ATTENDANCE_ALERT',  'Winnie Achieng has been marked inactive — review her record.',  false, 1500),
          ('PAYROLL-OFFICER', 'IN_APP', 'PAYROLL_COMPLETED', 'September payroll run completed. October run is in progress.',  false, 240),
          ('PAYROLL-OFFICER', 'IN_APP', 'LOAN_APPROVED',     'Loan request from Eunice Wanjiku is pending approval.',         false, 300),
          ('BRANCH-MANAGER',  'IN_APP', 'ATTENDANCE_ALERT',  '3 members of your team have not clocked in today.',             false, 60),
          ('BRANCH-MANAGER',  'IN_APP', 'LEAVE_APPROVED',    'A leave request from your branch is awaiting review.',          false, 200),
          ('FINANCE-OFFICER', 'EMAIL',  'PAYROLL_COMPLETED', 'September disbursement file is ready for release.',             false, 260),
          ('AGENT',           'IN_APP', 'PAYSLIP_AVAILABLE', 'Your September invoice has been issued.',                       false, 250),
          ('AUDITOR',         'IN_APP', 'ATTENDANCE_ALERT',  '2 manual attendance edits were recorded in the last 24 hours.', false, 400)
     ) AS v(username, channel, type, message, is_read, age_minutes)
         JOIN users u ON u.username = v.username
WHERE NOT EXISTS (
    SELECT 1 FROM notifications n
    WHERE n.recipient_user_id = u.id AND n.message = v.message
);

-- ------------------------------------------------------------
-- 21. Audit events.
--
--     `details_json` is left NULL. V2 converted that column to an
--     OID (a PostgreSQL large object) to satisfy Hibernate's
--     `@Lob` mapping, and writing one from a migration means
--     calling `lo_from_bytea` mid-Flyway-transaction — a real
--     failure mode for a cosmetic gain. The audit list renders
--     action / entity / actor fine without it, and rows written at
--     runtime by `@Auditable` do carry the before/after JSON.
-- ------------------------------------------------------------
INSERT INTO audit_logs (performed_by_user_id, action, entity_type, entity_id, details_json, created_at, updated_at)
SELECT u.id, v.action, v.entity_type, v.entity_id, NULL,
       now() - (v.age_minutes || ' minutes')::interval, now()
FROM (VALUES
          ('PAYROLL-OFFICER', 'PAYROLL_RUN_EXECUTED',    'PayrollRun',      1, 43200),
          ('PAYROLL-OFFICER', 'PAYSLIP_GENERATED',       'Payslip',         1, 43190),
          ('HR-MANAGER',      'EMPLOYEE_CREATED',        'Employee',        1, 57600),
          ('HR-MANAGER',      'EMPLOYEE_UPDATED',        'Employee',        1, 28800),
          ('HR-MANAGER',      'LEAVE_APPROVED',          'LeaveRequest',    1, 21600),
          ('ADMIN',           'BRANCH_CREATED',          'Branch',          1, 86400),
          ('ADMIN',           'USER_CREATED',            'User',            1, 72000),
          ('SUPER-ADMIN',     'USER_CREATED',            'User',            1, 120),
          ('SUPER-ADMIN',     'COMPANY_SETTINGS_UPDATED','CompanySettings', 1, 900),
          ('BRANCH-MANAGER',  'ATTENDANCE_MANUAL_ENTRY', 'Attendance',      1, 300),
          ('BRANCH-MANAGER',  'ATTENDANCE_MANUAL_ENTRY', 'Attendance',      1, 240),
          ('FINANCE-OFFICER', 'LOAN_APPROVED',           'Loan',            1, 43210),
          ('AUDITOR',         'AUDIT_EXPORTED',          'AuditLog',        1, 1440),
          ('AGENT',           'AGENT_ADVANCE_REQUESTED', 'AgentAdvance',    1, 25200)
     ) AS v(username, action, entity_type, entity_id, age_minutes)
         JOIN users u ON u.username = v.username
WHERE NOT EXISTS (
    SELECT 1 FROM audit_logs al
    WHERE al.performed_by_user_id = u.id AND al.action = v.action AND al.entity_type = v.entity_type
);
