-- The Admin or the Branch Manager can now give a day off too (when the agent isn't available), and to an employee
-- who has no agent. agent_id stays the employee's agent when there is one.
ALTER TABLE off_days ALTER COLUMN agent_id DROP NOT NULL;

-- Work time, set in Settings: arriving after the start is late, leaving before the end is early. Used by the
-- attendance record cards ("does this employee keep to work time").
ALTER TABLE company_settings ADD COLUMN IF NOT EXISTS work_start_time TIME NOT NULL DEFAULT '08:00';
ALTER TABLE company_settings ADD COLUMN IF NOT EXISTS work_end_time   TIME NOT NULL DEFAULT '17:00';
