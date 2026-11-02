-- src/main/resources/db/migration/V24__index_codes_and_deleted_users.sql
--
-- users     — a deleted login is hidden, not removed, so history keeps showing who did what.
-- branches  — codes become a plain index: 001, 002, 003.
-- employees — codes become a plain index across the company: EMP-0001, EMP-0002.
--
-- Biometric clock-ins are not affected: they find the employee through the terminal user id.
-- Each table goes through a temporary code first, because the codes are unique and one row's new
-- code can be another row's current one.

ALTER TABLE users
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE branches SET code = 'TMP-' || id;

UPDATE branches b
SET code = lpad(CAST(r.position AS TEXT), 3, '0')
FROM (SELECT id, row_number() OVER (ORDER BY id) AS position FROM branches) r
WHERE b.id = r.id;

UPDATE employees SET employee_number = 'TMP-' || id;

UPDATE employees e
SET employee_number = 'EMP-' || lpad(CAST(r.position AS TEXT), 4, '0')
FROM (SELECT id, row_number() OVER (ORDER BY id) AS position FROM employees) r
WHERE e.id = r.id;
