-- attendance_checkouts — when an employee leaves during the working day and is expected back: a lunch
-- break, or an emergency with the reason they gave. The day's first clock-in and last clock-out stay
-- on attendance. Only this table is added, so the biometric sync is not affected.
--
-- expected_back_at is set for a break with a time limit, such as lunch. checked_in_at stays empty until
-- they are back. check_type is free text, so another type needs no migration.

CREATE TABLE attendance_checkouts (
                                      id               BIGSERIAL PRIMARY KEY,
                                      created_at       TIMESTAMP,
                                      updated_at       TIMESTAMP,
                                      created_by       VARCHAR(255),
                                      updated_by       VARCHAR(255),

                                      attendance_id    BIGINT      NOT NULL REFERENCES attendance(id),
                                      check_type       VARCHAR(30) NOT NULL,
                                      checked_out_at   TIMESTAMP   NOT NULL,
                                      expected_back_at TIMESTAMP,
                                      checked_in_at    TIMESTAMP,
                                      reason           VARCHAR(255)
);

CREATE INDEX idx_attendance_checkouts_attendance ON attendance_checkouts (attendance_id);
