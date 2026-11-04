-- A login's own email address, for password resets. A login has had none until now.
ALTER TABLE users ADD COLUMN email VARCHAR(150);

-- An agent's login starts with the address on their agent record, when no other agent uses that address.
UPDATE users u
SET email = a.email
FROM agents a
WHERE u.agent_id = a.id
  AND u.deleted = false
  AND a.email IS NOT NULL
  AND btrim(a.email) <> ''
  AND (SELECT count(*) FROM agents other WHERE lower(other.email) = lower(a.email)) = 1;

-- A reset can be asked for by email, so one address must mean one login. Deleted logins do not count.
CREATE UNIQUE INDEX uk_users_email ON users (lower(email)) WHERE email IS NOT NULL AND deleted = false;

-- Apart from users, because every request loads the user and only a screen showing the photo needs it.
CREATE TABLE user_photos (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL UNIQUE REFERENCES users(id),
    content      BYTEA       NOT NULL,
    content_type VARCHAR(20) NOT NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP   NOT NULL DEFAULT now(),
    created_by   VARCHAR(255),
    updated_by   VARCHAR(255)
);

-- One forgotten-password attempt. Only hashes are stored: neither the code nor the token can be read back.
CREATE TABLE password_resets (
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT       NOT NULL REFERENCES users(id),
    code_hash              VARCHAR(100) NOT NULL,
    expires_at             TIMESTAMP    NOT NULL,
    attempts               INTEGER      NOT NULL DEFAULT 0,
    reset_token_hash       VARCHAR(64),
    reset_token_expires_at TIMESTAMP,
    used_at                TIMESTAMP,
    created_at             TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP    NOT NULL DEFAULT now(),
    created_by             VARCHAR(255),
    updated_by             VARCHAR(255)
);

CREATE INDEX idx_password_resets_user_id ON password_resets(user_id);
CREATE INDEX idx_password_resets_token ON password_resets(reset_token_hash);
