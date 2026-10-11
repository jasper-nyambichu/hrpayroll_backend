-- Lets a notification point at the thing it is about (e.g. an employee request),
-- so it can be opened from the bell and cleared when that thing is decided.
ALTER TABLE notifications ADD COLUMN reference_id BIGINT;

CREATE INDEX idx_notifications_type_reference ON notifications(type, reference_id);