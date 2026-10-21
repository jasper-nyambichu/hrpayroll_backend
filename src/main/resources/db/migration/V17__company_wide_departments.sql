-- Departments become company-wide: one list used by every branch.
ALTER TABLE departments ALTER COLUMN branch_id DROP NOT NULL;

-- Same-named departments (ignoring case and spacing) collapse into the oldest one.
CREATE TEMP TABLE department_merge ON COMMIT DROP AS
SELECT d.id AS old_id, kept.keep_id
FROM departments d
         JOIN (
    SELECT lower(trim(name)) AS dept_key, min(id) AS keep_id
    FROM departments
    GROUP BY lower(trim(name))
) kept ON lower(trim(d.name)) = kept.dept_key
WHERE d.id <> kept.keep_id;

-- Everything that pointed at a removed duplicate now points at the kept one.
UPDATE employees e SET department_id = m.keep_id
    FROM department_merge m WHERE e.department_id = m.old_id;

UPDATE shift_assignments s SET department_id = m.keep_id
    FROM department_merge m WHERE s.department_id = m.old_id;

UPDATE employee_creation_requests r SET department_id = m.keep_id
    FROM department_merge m WHERE r.department_id = m.old_id;

DELETE FROM departments WHERE id IN (SELECT old_id FROM department_merge);

UPDATE departments SET branch_id = NULL, name = trim(name);

CREATE UNIQUE INDEX IF NOT EXISTS uq_departments_name_ci ON departments (lower(name));