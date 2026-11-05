-- Scans at the reader now record lunch breaks and temporary check-outs. Each scan carries an event id, so a
-- retried sync from a PC's offline queue returns the row it already made instead of adding another.
-- Both columns start empty, so rows and clients that exist today are not affected.
--
-- expected_back_at on a temporary (EMERGENCY) check-out is only the time the employee said they would return.
-- Only a LUNCH can be late.

ALTER TABLE attendance_checkouts
    ADD COLUMN event_id VARCHAR(100),
    ADD COLUMN return_event_id VARCHAR(100);

CREATE UNIQUE INDEX uq_attendance_checkouts_event ON attendance_checkouts (event_id) WHERE event_id IS NOT NULL;
CREATE UNIQUE INDEX uq_attendance_checkouts_return_event ON attendance_checkouts (return_event_id) WHERE return_event_id IS NOT NULL;
