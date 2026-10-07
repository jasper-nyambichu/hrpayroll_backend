-- =============================================================================
-- V11 — demo dataset
-- =============================================================================
--
-- DESTINATION: hrpayroll_backend/src/main/resources/db/migration/
-- Copy this file there verbatim (the name matters: Flyway's separator is `__`,
-- two underscores, and V11 is the next free version after V10).
--
-- =============================================================================
--
-- Purpose: make every screen in the portal show real rows, and make /admin
-- reachable at all.
--
-- Before this migration the database holds one branch (HQ-001), one employee
-- (EMP-1001), one agent (Test Agent Ltd, V7) and eight staff accounts — and no
-- SUPER_ADMIN, so the entire /admin namespace is unreachable because
-- ROLE_HOME.SUPER_ADMIN and ROLE_NAMESPACE.SUPER_ADMIN both point at it.
--
-- Everything here is IDEMPOTENT. Every insert is keyed on a natural key and
-- carries ON CONFLICT DO NOTHING or a NOT EXISTS guard, so a re-run against a
-- partially-populated database is a no-op rather than a duplicate-key failure.
-- That matters because this doubles as the migration used to top up an
-- environment that already holds some rows.
--
-- Value vocabularies are taken from the Java entities, not invented. Two are
-- worth flagging because guessing them wrong fails silently rather than loudly:
--
--   employees.employment_status
--       ACTIVE | INACTIVE | TERMINATED — `Employee.employmentStatus`. Both
--       PayrollRunServiceImpl.resolveEmployees and BiometricSyncServiceImpl
--       gate on the literal "ACTIVE", so a plausible-looking value like
--       "PROBATION" would quietly exclude that employee from payroll and from
--       kiosk clock-ins. Nothing would error; the person would just vanish.
--
--   employees.employment_type
--       PERMANENT | CONTRACT | CASUAL | OUTSOURCED — V5 seeded 'FULL_TIME' and
--       V6 corrected it, which is how we know the first spelling was wrong.
--
--   notifications
--       channel EMAIL | SMS | IN_APP, type a free string — Notification.channel
--       and NotificationEventListener, which writes PAYROLL_COMPLETED and
--       LEAVE_APPROVED. There is no `title` column; the message carries it.
--
--   branches.status / agents.status / biometric_devices.status   ACTIVE | INACTIVE
--   attendance.source                                            MANUAL | BIOMETRIC
--   attendance.status                                            PRESENT | LATE | ABSENT
--   leave_requests.status      PENDING_MANAGER | PENDING_HR | APPROVED | REJECTED
--   loans.status                                     PENDING | APPROVED | REJECTED | CLOSED
--   payroll_runs.status                             IN_PROGRESS | COMPLETED | FAILED
--   agent_advances.status     OUTSTANDING | PARTIALLY_RECOVERED | FULLY_RECOVERED
--   agents.charge_type        PER_EMPLOYEE | PERCENTAGE_OF_GROSS | FIXED_MONTHLY | CUSTOM
--   shifts.type               MORNING | EVENING | NIGHT | ROTATING | FLEXIBLE
--
-- The payroll figures near the end are APPROXIMATIONS and are labelled as such
-- where they are written. See the comment above the payslip block.
-- =============================================================================


-- ----------------------------------------------------------------- company ---
-- companies is created in V1 and never inserted into, so /company, the
-- /admin/companies screens and every statutory rate read from an empty table.

INSERT INTO companies (name, address, phone, email, kra_pin, shif_number,
                       nssf_number, currency, timezone, created_at, updated_at)
SELECT 'Dhwarsh Holdings Ltd',
       'Dhwarsh House, Moi Avenue, Nairobi',
       '+254 20 555 0100',
       'info@dhwarsh.co.ke',
       'P051234567X',
       'SHIF-998877',
       'NSSF-112233',
       'KES',
       'Africa/Nairobi',
       now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM companies);

INSERT INTO company_settings (company_id, vat_rate, default_working_hours_per_day,
                              overtime_multiplier, weekend_days, payroll_period,
                              created_at, updated_at)
SELECT c.id, 16.00, 8.0, 1.5, 'SATURDAY,SUNDAY', 'MONTHLY', now(), now()
FROM companies c
WHERE NOT EXISTS (SELECT 1 FROM company_settings s WHERE s.company_id = c.id);


-- ------------------------------------------------------------ SUPER_ADMIN ---
-- The platform owner. Without this row nothing under /admin is reachable.
--
-- must_change_password is TRUE here, unlike every V4 account, and the asymmetry
-- is deliberate. Those are ordinary staff roles; this one can create users,
-- read the audit log and rewrite company settings. A standing super-admin login
-- whose password is committed to the repository is a backdoor, and forcing one
-- rotation on first sign-in is the whole difference between a demo account and
-- a permanent one. Sign in once as SUPER-ADMIN with the shared dev password,
-- set a new one, and the dashboard opens.
--
-- If a throwaway environment genuinely wants the shorter path, flip the flag —
-- but flip it knowingly.

INSERT INTO users (username, password, role, display_name, active,
                   must_change_password, created_at, updated_at)
SELECT 'SUPER-ADMIN',
       '$2b$10$ogv/i91wk.CwKv.4WEPAtuO9bEAnz.cx42V9xGBRuWmlh6EU0XMbO',
       'SUPER_ADMIN',
       'Platform Owner',
       true,
       true,
       now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'SUPER-ADMIN');


-- --------------------------------------------------------------- branches ---
-- Keeps V5's HQ-001 and adds six regional locations.

INSERT INTO branches (name, code, address, phone, email, status, created_at, updated_at)
VALUES
    ('Nairobi CBD',       'NBO-01', 'Kenyatta Avenue, Nairobi',     '+254 20 555 0201', 'nbo@dhwarsh.co.ke', 'ACTIVE', now(), now()),
    ('Mombasa Port',      'MSA-01', 'Moi Avenue, Mombasa',         '+254 41 555 0302', 'msa@dhwarsh.co.ke', 'ACTIVE', now(), now()),
    ('Kisumu Industrial', 'KSM-01', 'Industrial Area, Kisumu',     '+254 57 555 0403', 'ksm@dhwarsh.co.ke', 'ACTIVE', now(), now()),
    ('Nakuru Branch',     'NKR-01', 'Kenyatta Avenue, Nakuru',     '+254 51 555 0504', 'nkr@dhwarsh.co.ke', 'ACTIVE', now(), now()),
    ('Thika Road',        'THK-01', 'Thika Superhighway, Nairobi', '+254 20 555 0605', 'thk@dhwarsh.co.ke', 'ACTIVE', now(), now()),
    ('Eldoret Depot',     'ELD-01', 'Uganda Road, Eldoret',        '+254 53 555 0706', 'eld@dhwarsh.co.ke', 'ACTIVE', now(), now())
    ON CONFLICT (code) DO NOTHING;

-- The branch portal resolves its scope from users.branch_id. That column is
-- NULL for BRANCH-MANAGER today, so /branches/my-branch — and every screen
-- behind it — has nothing to resolve. Point it at the head office.

UPDATE users
SET branch_id = (SELECT id FROM branches WHERE code = 'HQ-001'),
    updated_at = now()
WHERE username = 'BRANCH-MANAGER'
  AND branch_id IS NULL
  AND EXISTS (SELECT 1 FROM branches WHERE code = 'HQ-001');


-- ------------------------------------------------------------ departments ---
-- departments has no unique constraint, so the guard is per (name, branch_id).

INSERT INTO departments (name, branch_id, created_at, updated_at)
SELECT v.name, b.id, now(), now()
FROM (VALUES
          ('Operations',      'HQ-001'),
          ('Human Resources', 'HQ-001'),
          ('Finance',         'HQ-001'),
          ('Operations',      'NBO-01'),
          ('Sales',           'NBO-01'),
          ('Logistics',       'MSA-01'),
          ('Port Operations', 'MSA-01'),
          ('Production',      'KSM-01'),
          ('Warehouse',       'NKR-01'),
          ('Sales',           'NKR-01'),
          ('Distribution',    'THK-01'),
          ('Stores',          'ELD-01')
     ) AS v(name, branch_code)
         JOIN branches b ON b.code = v.branch_code
WHERE NOT EXISTS (
    SELECT 1 FROM departments d WHERE d.name = v.name AND d.branch_id = b.id
);


-- ----------------------------------------------------------------- agents ---
-- V7 already seeded 'Test Agent Ltd' and linked the AGENT user to it, so these
-- are additional suppliers rather than a replacement.

INSERT INTO agents (name, contact_person, phone, email, address, bank_name,
                    bank_account_number, kra_pin, contract_start_date,
                    contract_end_date, charge_type, charge_amount,
                    charge_percentage, vat_registered, status,
                    created_at, updated_at)
SELECT v.name, v.contact_person, v.phone, v.email, v.address, v.bank_name,
       v.bank_account_number, v.kra_pin, v.start_date::date, v.end_date::date,
    v.charge_type,
       NULLIF(v.charge_amount, '')::numeric,
    NULLIF(v.charge_percentage, '')::numeric,
    v.vat_registered::boolean, v.status, now(), now()
FROM (VALUES
          ('Strategic Labour Ltd', 'Peter Njuguna', '+254722100001', 'ops@strategicl.co.ke',
           'Enterprise Road, Nairobi',  'Equity Bank', '0170123456789', 'P051111111A',
           '2023-01-01', '2026-12-31', 'PER_EMPLOYEE',         '1500.00', '',      'true',  'ACTIVE'),
          ('Coast Manpower Ltd',   'Ali Hassan',    '+254733200002', 'admin@coastmp.co.ke',
           'Nyerere Avenue, Mombasa',   'KCB Bank',    '1102233445566', 'P052222222B',
           '2023-06-01', '2026-06-30', 'PERCENTAGE_OF_GROSS',  '',        '12.50', 'true',  'ACTIVE'),
          ('Rift Valley Casuals',  'David Kimani',  '+254711300003', 'info@rvcasuals.co.ke',
           'Kenyatta Avenue, Nakuru',   'Co-op Bank',  '0113456789012', 'P053333333C',
           '2024-02-01', '2027-01-31', 'FIXED_MONTHLY',        '45000.00', '',     'false', 'ACTIVE')
     ) AS v(name, contact_person, phone, email, address, bank_name, bank_account_number,
            kra_pin, start_date, end_date, charge_type, charge_amount,
            charge_percentage, vat_registered, status)
WHERE NOT EXISTS (SELECT 1 FROM agents a WHERE a.name = v.name);


-- -------------------------------------------------------------- employees ---
--
-- The typed-VALUES trick: every column in the VALUES list is text, and the
-- casts in the SELECT give the real types. agent_name uses an empty string
-- rather than NULL because a single bare NULL makes Postgres unable to infer
-- that column's type across the whole list — NULLIF turns it back into "no
-- agent" for the directly-employed rows.
--
-- employee_number and national_id are both UNIQUE; the demo numbers start at
-- EMP-00412 to stay clear of V5's EMP-1001.

INSERT INTO employees (full_name, employee_number, national_id, date_of_birth,
                       gender, phone, email, kra_pin, shif_number, nssf_number,
                       branch_id, department_id, agent_id, position,
                       employment_type, basic_salary, date_hired,
                       employment_status, bank_name, bank_account_number,
                       emergency_contact_name, emergency_contact_phone,
                       created_at, updated_at)
SELECT v.full_name, v.employee_number, v.national_id, v.date_of_birth::date,
    v.gender, v.phone, v.email, v.kra_pin, v.shif_number, v.nssf_number,
       b.id, d.id, a.id, v.position,
       v.employment_type, v.basic_salary::numeric, v.date_hired::date,
    v.employment_status, v.bank_name, v.bank_account_number,
       v.emergency_contact_name, v.emergency_contact_phone,
       now(), now()
FROM (VALUES
          ('Grace Wanjiru',    'EMP-00412', '20100412', '1990-04-12', 'FEMALE', '+254711000412', 'grace.wanjiru@dhwarsh.co.ke',  'A010041234Z', 'SHIF-100412', 'NSSF-200412', 'NBO-01', 'Sales',           '',                     'Sales Executive',       'PERMANENT',  '85000.00',  '2021-03-01', 'ACTIVE',     'Equity Bank', '0170100412', 'John Wanjiru',     '+254711000413'),
          ('Peter Ochieng',    'EMP-00413', '20100413', '1988-08-23', 'MALE',   '+254722000413', 'peter.ochieng@dhwarsh.co.ke',  'A010041235Z', 'SHIF-100413', 'NSSF-200413', 'NBO-01', 'Operations',      '',                     'Operations Supervisor', 'PERMANENT',  '92000.00',  '2020-07-15', 'ACTIVE',     'KCB Bank',    '1102200413', 'Mary Ochieng',     '+254722000414'),
          ('Amina Yusuf',      'EMP-00414', '20100414', '1993-01-30', 'FEMALE', '+254733000414', 'amina.yusuf@dhwarsh.co.ke',    'A010041236Z', 'SHIF-100414', 'NSSF-200414', 'MSA-01', 'Port Operations', '',                     'Port Clerk',            'PERMANENT',  '68000.00',  '2022-02-01', 'ACTIVE',     'Co-op Bank',  '0113400414', 'Hassan Yusuf',     '+254733000415'),
          ('Brian Kamau',      'EMP-00415', '20100415', '1995-06-18', 'MALE',   '+254744000415', 'brian.kamau@dhwarsh.co.ke',    'A010041237Z', 'SHIF-100415', 'NSSF-200415', 'MSA-01', 'Logistics',       '',                     'Driver',                'CONTRACT',   '55000.00',  '2023-05-10', 'ACTIVE',     'Equity Bank', '0170100415', 'Alice Kamau',      '+254744000416'),
          ('Faith Njeri',      'EMP-00416', '20100416', '1991-11-05', 'FEMALE', '+254755000416', 'faith.njeri@dhwarsh.co.ke',    'A010041238Z', 'SHIF-100416', 'NSSF-200416', 'KSM-01', 'Production',      '',                     'Production Lead',       'PERMANENT',  '78000.00',  '2019-09-02', 'ACTIVE',     'KCB Bank',    '1102200416', 'Samuel Njeri',     '+254755000417'),
          ('David Mutua',      'EMP-00417', '20100417', '1987-03-14', 'MALE',   '+254766000417', 'david.mutua@dhwarsh.co.ke',    'A010041239Z', 'SHIF-100417', 'NSSF-200417', 'KSM-01', 'Production',      'Strategic Labour Ltd', 'Machine Operator',      'OUTSOURCED', '42000.00',  '2023-08-01', 'ACTIVE',     'Equity Bank', '0170100417', 'Esther Mutua',     '+254766000418'),
          ('Lucy Achieng',     'EMP-00418', '20100418', '1994-07-21', 'FEMALE', '+254777000418', 'lucy.achieng@dhwarsh.co.ke',   'A010041240Z', 'SHIF-100418', 'NSSF-200418', 'NKR-01', 'Warehouse',       '',                     'Warehouse Assistant',   'CASUAL',     '32000.00',  '2024-01-15', 'ACTIVE',     'Co-op Bank',  '0113400418', 'Rose Achieng',     '+254777000419'),
          ('Kevin Otieno',     'EMP-00419', '20100419', '1996-02-09', 'MALE',   '+254788000419', 'kevin.otieno@dhwarsh.co.ke',   'A010041241Z', 'SHIF-100419', 'NSSF-200419', 'NKR-01', 'Sales',           'Rift Valley Casuals',  'Sales Agent',           'OUTSOURCED', '38000.00',  '2024-03-04', 'ACTIVE',     'KCB Bank',    '1102200419', 'Janet Otieno',     '+254788000420'),
          ('Mercy Chebet',     'EMP-00420', '20100420', '1992-09-27', 'FEMALE', '+254799000420', 'mercy.chebet@dhwarsh.co.ke',   'A010041242Z', 'SHIF-100420', 'NSSF-200420', 'THK-01', 'Distribution',    '',                     'Dispatch Clerk',        'PERMANENT',  '62000.00',  '2022-10-03', 'ACTIVE',     'Equity Bank', '0170100420', 'Daniel Chebet',    '+254799000421'),
          ('Joseph Kiprop',    'EMP-00421', '20100421', '1989-12-11', 'MALE',   '+254710000421', 'joseph.kiprop@dhwarsh.co.ke',  'A010041243Z', 'SHIF-100421', 'NSSF-200421', 'ELD-01', 'Stores',          '',                     'Store Keeper',          'PERMANENT',  '58000.00',  '2021-06-21', 'ACTIVE',     'Co-op Bank',  '0113400421', 'Ruth Kiprop',      '+254710000422'),
          ('Hassan Ali',       'EMP-00422', '20100422', '1993-05-16', 'MALE',   '+254721000422', 'hassan.ali@dhwarsh.co.ke',     'A010041244Z', 'SHIF-100422', 'NSSF-200422', 'MSA-01', 'Logistics',       'Coast Manpower Ltd',   'Stevedore',             'OUTSOURCED', '45000.00',  '2023-11-06', 'ACTIVE',     'KCB Bank',    '1102200422', 'Fatuma Ali',       '+254721000423'),
          ('Rose Wambui',      'EMP-00423', '20100423', '1990-10-02', 'FEMALE', '+254732000423', 'rose.wambui@dhwarsh.co.ke',    'A010041245Z', 'SHIF-100423', 'NSSF-200423', 'HQ-001', 'Human Resources', '',                     'HR Officer',            'PERMANENT',  '88000.00',  '2020-04-06', 'ACTIVE',     'Equity Bank', '0170100423', 'Paul Wambui',      '+254732000424'),
          ('Samuel Mwangi',    'EMP-00424', '20100424', '1986-07-08', 'MALE',   '+254743000424', 'samuel.mwangi@dhwarsh.co.ke',  'A010041246Z', 'SHIF-100424', 'NSSF-200424', 'HQ-001', 'Finance',         '',                     'Accountant',            'PERMANENT', '105000.00',  '2018-02-12', 'ACTIVE',     'KCB Bank',    '1102200424', 'Nancy Mwangi',     '+254743000425'),
          ('Irene Nasimiyu',   'EMP-00425', '20100425', '1997-01-19', 'FEMALE', '+254754000425', 'irene.nasimiyu@dhwarsh.co.ke', 'A010041247Z', 'SHIF-100425', 'NSSF-200425', 'KSM-01', 'Production',      '',                     'Quality Checker',       'CONTRACT',   '48000.00',  '2024-05-20', 'ACTIVE',     'Co-op Bank',  '0113400425', 'Beatrice Nasimiyu', '+254754000426'),
          ('Thomas Barasa',    'EMP-00426', '20100426', '1985-04-25', 'MALE',   '+254765000426', 'thomas.barasa@dhwarsh.co.ke',  'A010041248Z', 'SHIF-100426', 'NSSF-200426', 'ELD-01', 'Stores',          '',                     'Loader',                'CASUAL',     '30000.00',  '2024-07-01', 'INACTIVE',   'Equity Bank', '0170100426', 'Agnes Barasa',     '+254765000427'),
          ('Cynthia Adhiambo', 'EMP-00427', '20100427', '1998-03-03', 'FEMALE', '+254776000427', 'cynthia.adhiambo@dhwarsh.co.ke','A010041249Z','SHIF-100427', 'NSSF-200427', 'NBO-01', 'Sales',           'Strategic Labour Ltd', 'Sales Promoter',        'OUTSOURCED', '40000.00',  '2024-09-02', 'ACTIVE',     'KCB Bank',    '1102200427', 'George Adhiambo',  '+254776000428'),
          ('Daniel Kones',     'EMP-00428', '20100428', '1991-08-14', 'MALE',   '+254787000428', 'daniel.kones@dhwarsh.co.ke',   'A010041250Z', 'SHIF-100428', 'NSSF-200428', 'NKR-01', 'Warehouse',       'Rift Valley Casuals',  'Forklift Operator',     'OUTSOURCED', '44000.00',  '2023-12-11', 'ACTIVE',     'Co-op Bank',  '0113400428', 'Jane Kones',       '+254787000429'),
          ('Esther Muthoni',   'EMP-00429', '20100429', '1994-12-30', 'FEMALE', '+254798000429', 'esther.muthoni@dhwarsh.co.ke', 'A010041251Z', 'SHIF-100429', 'NSSF-200429', 'THK-01', 'Distribution',    '',                     'Route Planner',         'PERMANENT',  '70000.00',  '2022-06-13', 'ACTIVE',     'Equity Bank', '0170100429', 'Simon Muthoni',    '+254798000430'),
          ('Patrick Omondi',   'EMP-00430', '20100430', '1989-06-07', 'MALE',   '+254709000430', 'patrick.omondi@dhwarsh.co.ke', 'A010041252Z', 'SHIF-100430', 'NSSF-200430', 'MSA-01', 'Logistics',       'Coast Manpower Ltd',   'Forklift Driver',       'OUTSOURCED', '46000.00',  '2024-02-19', 'ACTIVE',     'KCB Bank',    '1102200430', 'Lydia Omondi',     '+254709000431'),
          ('Naomi Wanjala',    'EMP-00431', '20100431', '1996-09-22', 'FEMALE', '+254720000431', 'naomi.wanjala@dhwarsh.co.ke',  'A010041253Z', 'SHIF-100431', 'NSSF-200431', 'HQ-001', 'Operations',      '',                     'Operations Analyst',    'PERMANENT',  '82000.00',  '2021-11-08', 'ACTIVE',     'Co-op Bank',  '0113400431', 'Moses Wanjala',    '+254720000432'),
          ('Victor Njoroge',   'EMP-00432', '20100432', '1992-02-17', 'MALE',   '+254731000432', 'victor.njoroge@dhwarsh.co.ke', 'A010041254Z', 'SHIF-100432', 'NSSF-200432', 'NBO-01', 'Operations',      '',                     'Logistics Officer',     'PERMANENT',  '76000.00',  '2022-08-15', 'ACTIVE',     'Equity Bank', '0170100432', 'Diana Njoroge',    '+254731000433'),
          ('Beatrice Kerubo',  'EMP-00433', '20100433', '1995-10-09', 'FEMALE', '+254742000433', 'beatrice.kerubo@dhwarsh.co.ke','A010041255Z', 'SHIF-100433', 'NSSF-200433', 'KSM-01', 'Production',      '',                     'Packer',                'CASUAL',     '31000.00',  '2024-08-05', 'ACTIVE',     'KCB Bank',    '1102200433', 'Peter Kerubo',     '+254742000434'),
          ('Stephen Lekakeny', 'EMP-00434', '20100434', '1990-05-28', 'MALE',   '+254753000434', 'stephen.lekakeny@dhwarsh.co.ke','A010041256Z','SHIF-100434', 'NSSF-200434', 'NKR-01', 'Sales',           '',                     'Regional Sales Lead',   'PERMANENT',  '95000.00',  '2020-01-20', 'ACTIVE',     'Co-op Bank',  '0113400434', 'Grace Lekakeny',   '+254753000435'),
          ('Janet Mueni',      'EMP-00435', '20100435', '1993-07-04', 'FEMALE', '+254764000435', 'janet.mueni@dhwarsh.co.ke',    'A010041257Z', 'SHIF-100435', 'NSSF-200435', 'THK-01', 'Distribution',    '',                     'Dispatch Supervisor',   'PERMANENT',  '72000.00',  '2023-03-27', 'TERMINATED', 'Equity Bank', '0170100435', 'Charles Mueni',    '+254764000436')
     ) AS v(full_name, employee_number, national_id, date_of_birth, gender, phone,
            email, kra_pin, shif_number, nssf_number, branch_code, department_name,
            agent_name, position, employment_type, basic_salary, date_hired,
            employment_status, bank_name, bank_account_number,
            emergency_contact_name, emergency_contact_phone)
         JOIN branches b ON b.code = v.branch_code
         LEFT JOIN departments d ON d.name = v.department_name AND d.branch_id = b.id
         LEFT JOIN agents a ON a.name = NULLIF(v.agent_name, '')
    ON CONFLICT (employee_number) DO NOTHING;

-- Department heads. One statement per department because the natural key is
-- (branch, department name) and the head is identified by employee number.
UPDATE departments d
SET head_of_department_id = e.id, updated_at = now()
    FROM employees e, branches b
WHERE e.employee_number = 'EMP-00423' AND b.code = 'HQ-001'
  AND d.branch_id = b.id AND d.name = 'Human Resources'
  AND d.head_of_department_id IS NULL;

UPDATE departments d
SET head_of_department_id = e.id, updated_at = now()
    FROM employees e, branches b
WHERE e.employee_number = 'EMP-00424' AND b.code = 'HQ-001'
  AND d.branch_id = b.id AND d.name = 'Finance'
  AND d.head_of_department_id IS NULL;

UPDATE departments d
SET head_of_department_id = e.id, updated_at = now()
    FROM employees e, branches b
WHERE e.employee_number = 'EMP-00413' AND b.code = 'NBO-01'
  AND d.branch_id = b.id AND d.name = 'Operations'
  AND d.head_of_department_id IS NULL;

UPDATE departments d
SET head_of_department_id = e.id, updated_at = now()
    FROM employees e, branches b
WHERE e.employee_number = 'EMP-00416' AND b.code = 'KSM-01'
  AND d.branch_id = b.id AND d.name = 'Production'
  AND d.head_of_department_id IS NULL;


-- -------------------------------------------------------- biometric devices ---
--
-- last_sync_at is staggered on purpose. The backend computes `online` against a
-- five-minute freshness window, so a fleet where every terminal synced at the
-- same instant would hide the offline state the devices screen exists to show.
--
-- device_token_hash is NOT NULL. These are placeholders, not usable tokens:
-- real enrolment mints a hash from a token shown once at registration.

INSERT INTO biometric_devices (branch_id, device_serial, device_token_hash,
                               last_sync_at, status, created_at, updated_at)
SELECT b.id, v.device_serial, v.device_token_hash,
       now() - (v.minutes_ago || ' minutes')::interval, v.status, now(), now()
FROM (VALUES
          ('HQ-001', 'ZK-HQ-0001',  'seed-placeholder-hq-0001',  '2',    'ACTIVE'),
          ('NBO-01', 'ZK-NBO-0002', 'seed-placeholder-nbo-0002', '1',    'ACTIVE'),
          ('MSA-01', 'ZK-MSA-0003', 'seed-placeholder-msa-0003', '240',  'ACTIVE'),
          ('KSM-01', 'ZK-KSM-0004', 'seed-placeholder-ksm-0004', '3',    'ACTIVE'),
          ('NKR-01', 'ZK-NKR-0005', 'seed-placeholder-nkr-0005', '12',   'ACTIVE'),
          ('THK-01', 'ZK-THK-0006', 'seed-placeholder-thk-0006', '1',    'ACTIVE'),
          ('ELD-01', 'ZK-ELD-0007', 'seed-placeholder-eld-0007', '1440', 'ACTIVE')
     ) AS v(branch_code, device_serial, device_token_hash, minutes_ago, status)
         JOIN branches b ON b.code = v.branch_code
    ON CONFLICT (device_serial) DO NOTHING;


-- -------------------------------------------------------------- attendance ---
--
-- Three weeks of weekday history for every active employee, so the attendance
-- board has a real rate, a real live view and a real per-branch roll-up.
--
-- random() is drawn once per (employee, day) in the `grid` CTE and then reused
-- by every derived column. Calling it separately inside each CASE would give
-- each column an independent draw, which produces rows that are LATE but have
-- no clock-in — impossible data, and precisely the kind of thing that makes a
-- demo dataset untrustworthy the moment somebody reconciles against it.
--
-- clock_out_event_id is deliberately left NULL: it carries a UNIQUE constraint
-- and is written by the kiosk on the clock-out scan, not seeded.

WITH days AS (
    SELECT d::date AS day
FROM generate_series(current_date - INTERVAL '20 days', current_date, INTERVAL '1 day') AS d
WHERE EXTRACT(ISODOW FROM d) < 6
    ),
    grid AS (
SELECT e.id AS employee_id,
    e.branch_id,
    e.employee_number,
    dd.day,
    random() AS r
FROM employees e
    CROSS JOIN days dd
WHERE e.employment_status = 'ACTIVE'
    )
INSERT INTO attendance (employee_id, branch_id, attendance_date, clock_in,
                        clock_out, source, status, event_id, created_at, updated_at)
SELECT employee_id,
       branch_id,
    day,
    CASE WHEN r < 0.06 THEN NULL
    ELSE day + TIME '08:00' + (r * INTERVAL '50 minutes') END,
       CASE WHEN r < 0.06 THEN NULL
            ELSE day + TIME '17:00' + (r * INTERVAL '45 minutes') END,
       CASE WHEN r < 0.82 THEN 'BIOMETRIC' ELSE 'MANUAL' END,
       CASE WHEN r < 0.06 THEN 'ABSENT'
            WHEN r < 0.16 THEN 'LATE'
            ELSE 'PRESENT' END,
       'SEED-' || employee_number || '-' || to_char(day, 'YYYYMMDD'),
       now(), now()
FROM grid
ON CONFLICT (event_id) DO NOTHING;


-- ------------------------------------------------------------- leave types ---
-- leave_types.name is UNIQUE, so ON CONFLICT works directly.

INSERT INTO leave_types (name, default_entitlement_days, paid, created_at, updated_at)
VALUES
    ('Annual Leave',        21, true,  now(), now()),
    ('Sick Leave',          14, true,  now(), now()),
    ('Maternity Leave',     90, true,  now(), now()),
    ('Paternity Leave',     14, true,  now(), now()),
    ('Compassionate Leave',  5, true,  now(), now()),
    ('Study Leave',         10, false, now(), now())
    ON CONFLICT (name) DO NOTHING;


-- ------------------------------------------------------------------ shifts ---

INSERT INTO shifts (name, type, start_time, end_time, created_at, updated_at)
SELECT v.name, v.type, v.start_time::time, v.end_time::time, now(), now()
FROM (VALUES
          ('Day Shift',   'MORNING',  '08:00', '17:00'),
          ('Night Shift', 'NIGHT',    '20:00', '05:00'),
          ('Split Shift', 'FLEXIBLE', '10:00', '19:00')
     ) AS v(name, type, start_time, end_time)
WHERE NOT EXISTS (SELECT 1 FROM shifts s WHERE s.name = v.name);

-- Assign the day shift to every active employee at their own branch, so
-- /shifts/assignments/branch/{id} returns a full roster rather than an empty
-- table. shift_assignments has no unique constraint, hence the NOT EXISTS.
INSERT INTO shift_assignments (shift_id, employee_id, department_id, branch_id,
                               created_at, updated_at)
SELECT s.id, e.id, e.department_id, e.branch_id, now(), now()
FROM employees e
         CROSS JOIN shifts s
WHERE s.name = 'Day Shift'
  AND e.employment_status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1 FROM shift_assignments sa
    WHERE sa.shift_id = s.id AND sa.employee_id = e.id
);


-- ---------------------------------------------------------- leave requests ---
--
-- Dates are relative to the day this migration runs, so the PENDING rows sit in
-- the future while the short-offset APPROVED ones overlap today — which is what
-- makes an "on leave today" panel non-empty rather than perpetually blank.

INSERT INTO leave_requests (employee_id, leave_type_id, start_date, end_date,
                            reason, status, created_at, updated_at)
SELECT e.id, lt.id,
       (current_date + (v.day_offset_start || ' days')::interval)::date,
    (current_date + (v.day_offset_end || ' days')::interval)::date,
    v.reason, v.status, now(), now()
FROM (VALUES
          ('EMP-00412', 'Annual Leave',       '14', '18', 'Family holiday.',              'PENDING_MANAGER'),
          ('EMP-00418', 'Sick Leave',          '2',  '4', 'Medical certificate attached.', 'APPROVED'),
          ('EMP-00420', 'Compassionate Leave', '9', '11', 'Bereavement.',                  'PENDING_HR'),
          ('EMP-00429', 'Annual Leave',        '21', '28', 'Annual break.',                 'APPROVED'),
          ('EMP-00415', 'Study Leave',         '30', '44', 'Professional certification.',   'REJECTED'),
          ('EMP-00433', 'Sick Leave',           '5',  '6', 'Flu.',                         'PENDING_MANAGER')
     ) AS v(employee_number, leave_type, day_offset_start, day_offset_end, reason, status)
         JOIN employees e ON e.employee_number = v.employee_number
         JOIN leave_types lt ON lt.name = v.leave_type
WHERE NOT EXISTS (
    SELECT 1 FROM leave_requests lr
    WHERE lr.employee_id = e.id
      AND lr.leave_type_id = lt.id
      AND lr.start_date = (current_date + (v.day_offset_start || ' days')::interval)::date
);


-- ---------------------------------------------------------- agent advances ---
-- agent_advances has no unique constraint, so the guard is on (agent, purpose).

INSERT INTO agent_advances (agent_id, amount, advance_date, purpose,
                            outstanding_balance, status, created_at, updated_at)
SELECT a.id, v.amount::numeric,
    (current_date - (v.days_ago || ' days')::interval)::date,
    v.purpose, v.outstanding::numeric, v.status, now(), now()
FROM (VALUES
          ('Strategic Labour Ltd', '120000.00', '45', 'Mobilisation for the Nairobi rollout.', '120000.00', 'OUTSTANDING'),
          ('Coast Manpower Ltd',   '80000.00',  '20', 'Advance against this month''s deployment.', '35000.00', 'PARTIALLY_RECOVERED'),
          ('Rift Valley Casuals',  '50000.00',  '90', 'Seasonal hiring float.',                '0.00',      'FULLY_RECOVERED')
     ) AS v(agent_name, amount, days_ago, purpose, outstanding, status)
         JOIN agents a ON a.name = v.agent_name
WHERE NOT EXISTS (
    SELECT 1 FROM agent_advances aa WHERE aa.agent_id = a.id AND aa.purpose = v.purpose
);


-- ------------------------------------------------------------------- loans ---

INSERT INTO loans (employee_id, principal_amount, interest_rate,
                   installment_amount, outstanding_balance, request_date,
                   approval_date, status, created_at, updated_at)
SELECT e.id, v.principal::numeric, v.rate::numeric, v.installment::numeric,
    v.outstanding::numeric,
    (current_date - (v.days_ago || ' days')::interval)::date,
    (current_date - ((v.days_ago::int - 3) || ' days')::interval)::date,
    v.status, now(), now()
FROM (VALUES
          ('EMP-00413', '300000.00', '8.00',  '25000.00', '175000.00', '120', 'APPROVED'),
          ('EMP-00416', '150000.00', '8.00',  '12500.00', '50000.00',  '75',  'APPROVED'),
          ('EMP-00420', '100000.00', '10.00', '10000.00', '0.00',      '200', 'CLOSED'),
          ('EMP-00421', '200000.00', '8.00',  '16666.67', '200000.00', '10',  'PENDING')
     ) AS v(employee_number, principal, rate, installment, outstanding, days_ago, status)
         JOIN employees e ON e.employee_number = v.employee_number
WHERE NOT EXISTS (
    SELECT 1 FROM loans l
    WHERE l.employee_id = e.id AND l.principal_amount = v.principal::numeric
);


-- ----------------------------------------------------------------- payroll ---
--
-- Two runs so /payroll/runs is non-empty in two states: a COMPLETED run for the
-- previous month carrying payslips, and the current month IN_PROGRESS with none
-- yet — which is what a run looks like mid-flight.
--
-- ⚠ THE FIGURES ON THE PAYSLIPS BELOW ARE APPROXIMATIONS, NOT COMPLIANT
-- CALCULATIONS. The real engine computes statutory deductions in
-- PayrollRunServiceImpl from the rates on company_settings, including graduated
-- PAYE bands and the NSSF tier split. Reproducing that in SQL would create a
-- second, subtly-different implementation of the payroll engine — exactly the
-- kind of divergence that ships as a bug.
--
-- So: flat rates, applied to a gross derived from each employee's real
-- basic_salary, with personal relief subtracted. The right order of magnitude
-- for laying out the screens. They are not returns, and nobody should remit
-- against them. Re-running payroll through the API replaces them with engine
-- output.

INSERT INTO payroll_runs (period_start, period_end, status, total_gross_payroll,
                          total_net_payroll, created_at, updated_at)
SELECT date_trunc('month', current_date - INTERVAL '1 month')::date,
    (date_trunc('month', current_date) - INTERVAL '1 day')::date,
    'COMPLETED', 0, 0, now(), now()
    WHERE NOT EXISTS (
  SELECT 1 FROM payroll_runs
  WHERE period_start = date_trunc('month', current_date - INTERVAL '1 month')::date
);

INSERT INTO payroll_runs (period_start, period_end, status, created_at, updated_at)
SELECT date_trunc('month', current_date)::date,
    (date_trunc('month', current_date) + INTERVAL '1 month' - INTERVAL '1 day')::date,
    'IN_PROGRESS', now(), now()
    WHERE NOT EXISTS (
  SELECT 1 FROM payroll_runs
  WHERE period_start = date_trunc('month', current_date)::date
);

-- One payslip per active employee on the completed run.
INSERT INTO payslips (payroll_run_id, employee_id, basic_salary,
                      house_allowance, transport_allowance, overtime_pay, bonus,
                      other_allowances, gross_pay, shif_deduction,
                      nssf_deduction, housing_levy_deduction, paye_deduction,
                      nita_deduction, loan_deduction, other_deductions, net_pay,
                      created_at, updated_at)
SELECT r.id,
       e.id,
       e.basic_salary,
       round(e.basic_salary * 0.15, 2),                                   -- house allowance
       5000.00,                                                           -- transport allowance
       0.00, 0.00, 0.00,
       round(e.basic_salary * 1.15, 2) + 5000.00,                         -- gross
       round((e.basic_salary * 1.15 + 5000) * 0.0275, 2),                 -- SHIF 2.75%
       least(round((e.basic_salary * 1.15 + 5000) * 0.06, 2), 2160.00),   -- NSSF 6%, capped
       round((e.basic_salary * 1.15 + 5000) * 0.015, 2),                  -- Housing Levy 1.5%
       greatest(0, round(((e.basic_salary * 1.15 + 5000) * 0.30) - 2400, 2)),  -- PAYE 30% less relief
       0.00,
       coalesce((SELECT round(l.installment_amount, 2) FROM loans l
                 WHERE l.employee_id = e.id AND l.status = 'APPROVED'
                 ORDER BY l.id LIMIT 1), 0.00),
       0.00,
       (e.basic_salary * 1.15 + 5000)
           - round((e.basic_salary * 1.15 + 5000) * 0.0275, 2)
           - least(round((e.basic_salary * 1.15 + 5000) * 0.06, 2), 2160.00)
           - round((e.basic_salary * 1.15 + 5000) * 0.015, 2)
           - greatest(0, round(((e.basic_salary * 1.15 + 5000) * 0.30) - 2400, 2))
           - coalesce((SELECT round(l.installment_amount, 2) FROM loans l
                       WHERE l.employee_id = e.id AND l.status = 'APPROVED'
                       ORDER BY l.id LIMIT 1), 0.00),
       now(), now()
FROM employees e
         CROSS JOIN payroll_runs r
WHERE r.period_start = date_trunc('month', current_date - INTERVAL '1 month')::date
  AND e.employment_status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1 FROM payslips p WHERE p.payroll_run_id = r.id AND p.employee_id = e.id
  );

-- Roll the run totals up from the payslips just written.
UPDATE payroll_runs r
SET total_gross_payroll = t.gross,
    total_net_payroll   = t.net,
    updated_at          = now()
    FROM (
  SELECT payroll_run_id, sum(gross_pay) AS gross, sum(net_pay) AS net
  FROM payslips
  GROUP BY payroll_run_id
) t
WHERE r.id = t.payroll_run_id
  AND r.status = 'COMPLETED'
  AND coalesce(r.total_gross_payroll, 0) = 0;

-- Repayments applying the completed run against the approved loans.
INSERT INTO loan_repayments (loan_id, payroll_run_id, amount_paid, created_at, updated_at)
SELECT l.id, r.id, round(l.installment_amount, 2), now(), now()
FROM loans l
         CROSS JOIN payroll_runs r
WHERE l.status = 'APPROVED'
  AND r.period_start = date_trunc('month', current_date - INTERVAL '1 month')::date
  AND NOT EXISTS (
    SELECT 1 FROM loan_repayments lr
    WHERE lr.loan_id = l.id AND lr.payroll_run_id = r.id
  );

-- Agent summaries for the outsourced employees on the completed run. The
-- service charge is computed per charge_type against the agent's real terms,
-- so the three seeded agents each exercise a different branch of that CASE.
INSERT INTO agent_payroll_summaries (payroll_run_id, agent_id, employee_count,
                                     gross_salaries, service_charge, shif_total,
                                     nssf_total, housing_levy_total, paye_total,
                                     employee_loan_deductions_total,
                                     advance_recovery_total, subtotal_before_vat,
                                     vat_applied, vat_rate_used, vat_amount,
                                     final_amount_payable, created_at, updated_at)
SELECT p.payroll_run_id,
       e.agent_id,
       count(DISTINCT e.id),
       sum(p.gross_pay),
       CASE a.charge_type
           WHEN 'PER_EMPLOYEE'        THEN a.charge_amount * count(DISTINCT e.id)
           WHEN 'PERCENTAGE_OF_GROSS' THEN round(sum(p.gross_pay) * a.charge_percentage / 100, 2)
           ELSE a.charge_amount
           END,
       sum(p.shif_deduction),
       sum(p.nssf_deduction),
       sum(p.housing_levy_deduction),
       sum(p.paye_deduction),
       sum(p.loan_deduction),
       0.00,
       sum(p.gross_pay) + CASE a.charge_type
                              WHEN 'PER_EMPLOYEE'        THEN a.charge_amount * count(DISTINCT e.id)
                              WHEN 'PERCENTAGE_OF_GROSS' THEN round(sum(p.gross_pay) * a.charge_percentage / 100, 2)
                              ELSE a.charge_amount
           END,
       a.vat_registered,
       16.00,
       CASE WHEN a.vat_registered
                THEN round((sum(p.gross_pay) + CASE a.charge_type
                                                   WHEN 'PER_EMPLOYEE'        THEN a.charge_amount * count(DISTINCT e.id)
                                                   WHEN 'PERCENTAGE_OF_GROSS' THEN round(sum(p.gross_pay) * a.charge_percentage / 100, 2)
                                                   ELSE a.charge_amount END) * 0.16, 2)
            ELSE 0.00 END,
       (sum(p.gross_pay) + CASE a.charge_type
                               WHEN 'PER_EMPLOYEE'        THEN a.charge_amount * count(DISTINCT e.id)
                               WHEN 'PERCENTAGE_OF_GROSS' THEN round(sum(p.gross_pay) * a.charge_percentage / 100, 2)
                               ELSE a.charge_amount
           END)
           + CASE WHEN a.vat_registered
                      THEN round((sum(p.gross_pay) + CASE a.charge_type
                                                         WHEN 'PER_EMPLOYEE'        THEN a.charge_amount * count(DISTINCT e.id)
                                                         WHEN 'PERCENTAGE_OF_GROSS' THEN round(sum(p.gross_pay) * a.charge_percentage / 100, 2)
                                                         ELSE a.charge_amount END) * 0.16, 2)
                  ELSE 0.00 END,
       now(), now()
FROM payslips p
         JOIN employees e ON e.id = p.employee_id
         JOIN agents a ON a.id = e.agent_id
WHERE e.agent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM agent_payroll_summaries s
    WHERE s.payroll_run_id = p.payroll_run_id AND s.agent_id = e.agent_id
)
GROUP BY p.payroll_run_id, e.agent_id, a.charge_type, a.charge_amount,
         a.charge_percentage, a.vat_registered;


-- ----------------------------------------------------------- notifications ---
--
-- The topbar bell reads this. `channel` is IN_APP, matching what
-- NotificationEventListener writes; there is no title column, so the message
-- stands alone. Joined on username, so a missing account makes the block a
-- no-op rather than leaving an orphaned row.

INSERT INTO notifications (recipient_user_id, channel, type, message, read,
                           created_at, updated_at)
SELECT u.id, 'IN_APP', v.type, v.message, false,
       now() - (v.minutes_ago || ' minutes')::interval, now()
FROM (VALUES
          ('SUPER-ADMIN',    'COMPANY_CREATED',    'Dhwarsh Holdings Ltd was registered as the organization.',      '5'),
          ('SUPER-ADMIN',    'DEVICE_ENROLLED',    'Seven biometric terminals are now reporting across branches.',  '30'),
          ('ADMIN',          'ATTENDANCE_IMPORTED','Three weeks of attendance history is available.',               '10'),
          ('ADMIN',          'AGENT_REGISTERED',   'Three labour agents were added to the supplier register.',      '45'),
          ('HR-MANAGER',     'EMPLOYEE_IMPORTED',  'The staff register was extended with the demo roster.',         '15'),
          ('PAYROLL-OFFICER','PAYROLL_COMPLETED',  'Payroll for last month completed and payslips are available.',  '60'),
          ('FINANCE-OFFICER','INVOICE_READY',      'Agent payroll summaries are ready for last month.',             '70'),
          ('BRANCH-MANAGER', 'BRANCH_ASSIGNED',    'Your account is now linked to the head office branch.',         '20'),
          ('AUDITOR',        'AUDIT_READY',        'The audit log holds entries from the demo seed.',               '25')
     ) AS v(username, type, message, minutes_ago)
         JOIN users u ON u.username = v.username
WHERE NOT EXISTS (
    SELECT 1 FROM notifications n
    WHERE n.recipient_user_id = u.id AND n.message = v.message
);


-- ------------------------------------------------------------------ audit ---
-- A short trail so /audit/logs is not empty. details_json is TEXT (V2).
-- The guard keys on the details payload as well as the action, so two entries
-- sharing an (actor, action, entity_type) triple both insert on the first run
-- and both stay put afterwards.

INSERT INTO audit_logs (performed_by_user_id, action, entity_type, entity_id,
                        details_json, created_at, updated_at)
SELECT u.id, v.action, v.entity_type, NULLIF(v.entity_id, '')::bigint,
    v.details, now() - (v.days_ago || ' days')::interval, now()
FROM (VALUES
          ('SUPER-ADMIN',    'CREATE', 'Company',         '',  '{"name":"Dhwarsh Holdings Ltd"}',        '4'),
          ('ADMIN',          'CREATE', 'Branch',          '',  '{"code":"NBO-01","name":"Nairobi CBD"}', '3'),
          ('ADMIN',          'CREATE', 'Branch',          '',  '{"code":"MSA-01","name":"Mombasa Port"}','3'),
          ('ADMIN',          'CREATE', 'BiometricDevice', '',  '{"enrolled":7}',                         '3'),
          ('HR-MANAGER',     'CREATE', 'Employee',        '',  '{"imported":24}',                        '2'),
          ('HR-MANAGER',     'UPDATE', 'Employee',        '',  '{"employmentType":"PERMANENT"}',         '2'),
          ('PAYROLL-OFFICER','CREATE', 'PayrollRun',      '',  '{"status":"COMPLETED"}',                 '1'),
          ('BRANCH-MANAGER', 'UPDATE', 'Attendance',      '',  '{"source":"MANUAL"}',                    '1')
     ) AS v(username, action, entity_type, entity_id, details, days_ago)
         JOIN users u ON u.username = v.username
WHERE NOT EXISTS (
    SELECT 1 FROM audit_logs al
    WHERE al.performed_by_user_id = u.id
      AND al.action = v.action
      AND al.entity_type = v.entity_type
      AND al.details_json = v.details
);
