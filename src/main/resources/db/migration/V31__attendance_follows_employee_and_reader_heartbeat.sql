-- 1. An employee's attendance follows them when they move branch: their whole history shows under the branch
--    they work at now, and keeps counting towards their pay (days worked x 1000) there.
--    Where each scan was actually taken is kept in scanned_branch_id, so nothing about the past is lost.
ALTER TABLE attendance ADD COLUMN scanned_branch_id BIGINT;
UPDATE attendance SET scanned_branch_id = branch_id;

-- New rows remember where they were scanned, whatever code inserts them.
CREATE OR REPLACE FUNCTION attendance_keep_scanned_branch() RETURNS trigger AS $$
BEGIN
    IF NEW.scanned_branch_id IS NULL THEN
        NEW.scanned_branch_id := NEW.branch_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER attendance_scanned_branch
    BEFORE INSERT ON attendance
    FOR EACH ROW EXECUTE FUNCTION attendance_keep_scanned_branch();

-- Employees already moved before this release: bring their history to their current branch.
UPDATE attendance a
SET branch_id = e.branch_id
FROM employees e
WHERE a.employee_id = e.id
  AND e.branch_id IS NOT NULL
  AND a.branch_id IS DISTINCT FROM e.branch_id;

-- 2. Each branch PC reports every minute, and at once when its reader is plugged in or out, so the web shows a
--    reader that is unplugged, or a PC that has gone quiet, even when nobody is scanning.
ALTER TABLE biometric_devices ADD COLUMN last_seen_at TIMESTAMP;
ALTER TABLE biometric_devices ADD COLUMN reader_connected BOOLEAN;
ALTER TABLE biometric_devices ADD COLUMN reader_changed_at TIMESTAMP;
ALTER TABLE biometric_devices ADD COLUMN app_version VARCHAR(30);
ALTER TABLE biometric_devices ADD COLUMN pending_scans INTEGER;
