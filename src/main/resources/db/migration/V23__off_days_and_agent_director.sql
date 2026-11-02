-- src/main/resources/db/migration/V23__off_days_and_agent_director.sql
--
-- off_days           — days an agent gives one of their employees off. A separate table from
--                      leave_requests on purpose: biometric sync refuses a clock-in during approved
--                      leave, and an off day must not change what the terminals accept.
-- agents             — the director's name.
-- agent_director_ids — a photo of the director's ID. Kept apart from agents so that loading an agent
--                      (which every payment does) never loads the image.

CREATE TABLE off_days (
                          id          BIGSERIAL PRIMARY KEY,
                          created_at  TIMESTAMP,
                          updated_at  TIMESTAMP,
                          created_by  VARCHAR(255),
                          updated_by  VARCHAR(255),

                          employee_id BIGINT NOT NULL REFERENCES employees(id),
                          agent_id    BIGINT NOT NULL REFERENCES agents(id),
                          start_date  DATE   NOT NULL,
                          end_date    DATE   NOT NULL,
                          reason      VARCHAR(255),

                          CONSTRAINT ck_off_days_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_off_days_dates ON off_days(start_date, end_date);
CREATE INDEX idx_off_days_employee ON off_days(employee_id);
CREATE INDEX idx_off_days_agent ON off_days(agent_id);

ALTER TABLE agents
    ADD COLUMN IF NOT EXISTS director_name VARCHAR(150);

CREATE TABLE agent_director_ids (
                                    id           BIGSERIAL PRIMARY KEY,
                                    created_at   TIMESTAMP,
                                    updated_at   TIMESTAMP,
                                    created_by   VARCHAR(255),
                                    updated_by   VARCHAR(255),

                                    agent_id     BIGINT      NOT NULL UNIQUE REFERENCES agents(id),
                                    content      BYTEA       NOT NULL,
                                    content_type VARCHAR(20) NOT NULL
);
