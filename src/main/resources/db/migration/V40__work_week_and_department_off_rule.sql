-- V40: how the organisation operates, editable under Admin → Settings.
--
-- 1. The work week: for each day, whether the organisation works and from when to when. Late arrivals and early
--    leaving are measured against that day's hours, and a day it does not work is not counted as a missed day.
--    Starts from the hours already saved (work_start_time / work_end_time), every day open, except:
--      Shah Ceramics: Monday–Saturday 08:00–18:00, Sunday 08:00–13:00.
-- 2. Off days: whether only one employee of a department (in the same branch) may be off on the same day.
--    On for Shah Ceramics, off for everyone else (Kisiimatt).
-- Additive only.

CREATE TABLE IF NOT EXISTS work_week_days (
    id          BIGSERIAL PRIMARY KEY,
    day_of_week VARCHAR(10) NOT NULL UNIQUE,   -- MONDAY … SUNDAY
    working     BOOLEAN     NOT NULL DEFAULT TRUE,
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255),
    CONSTRAINT chk_work_week_day CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    CONSTRAINT chk_work_week_hours CHECK (end_time > start_time)
);

INSERT INTO work_week_days (day_of_week, working, start_time, end_time, created_at, updated_at, created_by, updated_by)
SELECT d.day, TRUE,
       COALESCE((SELECT work_start_time FROM company_settings LIMIT 1), TIME '08:00'),
       COALESCE((SELECT work_end_time   FROM company_settings LIMIT 1), TIME '17:00'),
       now(), now(), 'V40', 'V40'
  FROM (VALUES ('MONDAY'),('TUESDAY'),('WEDNESDAY'),('THURSDAY'),('FRIDAY'),('SATURDAY'),('SUNDAY')) AS d(day)
ON CONFLICT (day_of_week) DO NOTHING;

-- Shah Ceramics: Monday to Saturday 8 am – 6 pm, Sunday 8 am – 1 pm.
UPDATE work_week_days SET working = TRUE, start_time = TIME '08:00',
       end_time = CASE WHEN day_of_week = 'SUNDAY' THEN TIME '13:00' ELSE TIME '18:00' END
 WHERE EXISTS (SELECT 1 FROM companies WHERE upper(name) LIKE '%SHAH%');

ALTER TABLE company_settings ADD COLUMN IF NOT EXISTS one_off_per_department BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE company_settings SET one_off_per_department = TRUE
 WHERE company_id IN (SELECT id FROM companies WHERE upper(name) LIKE '%SHAH%');
