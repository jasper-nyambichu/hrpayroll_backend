-- The password the Admin last set for a login, encrypted, so the Admin can look it up again after creating it.
-- Cleared as soon as the person changes or resets it themselves, because the Admin no longer knows it then.
ALTER TABLE users ADD COLUMN issued_password_enc BYTEA;
ALTER TABLE users ADD COLUMN issued_password_at TIMESTAMP;
ALTER TABLE users ADD COLUMN password_changed_by_owner_at TIMESTAMP;
