ALTER TABLE attendance ADD COLUMN IF NOT EXISTS clock_out_event_id VARCHAR(100) UNIQUE;

CREATE INDEX IF NOT EXISTS idx_attendance_attendance_date ON attendance (attendance_date);
CREATE INDEX IF NOT EXISTS idx_attendance_clock_in ON attendance (clock_in);