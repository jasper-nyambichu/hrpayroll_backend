-- users.can_run_payments — the Admin lets a Branch Manager run the agent payment calculation while
-- the Admin is away, and can take it back. Off for everyone until the Admin grants it.

ALTER TABLE users ADD COLUMN can_run_payments BOOLEAN NOT NULL DEFAULT FALSE;
