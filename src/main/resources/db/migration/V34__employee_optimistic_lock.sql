-- Optimistic locking for employee records (Employee.version, JPA @Version).
-- Every save raises the number by one; a save made from an out-of-date copy is refused, so two admins editing
-- the same employee can no longer silently overwrite each other. Existing rows start at 0. Additive only:
-- no existing column or row changes.
ALTER TABLE employees ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
