-- V39: the Accounts Office portal (role FINANCE_OFFICER), for the organisations that use it.
--
-- 1. A switch per organisation, OFF by default. Only Kisiimatt has it on: every other organisation on this code
--    (e.g. Shah Ceramics) keeps working exactly as before, with the Admin releasing agent payments.
-- 2. How each agent payment was paid, with the proof: M-Pesa (transaction code), cheque (number, bank, date) or
--    manual (a description / reference), the date it was paid, a note, and an optional scan or photo.
-- Additive only: no existing column changes meaning.

ALTER TABLE company_settings ADD COLUMN IF NOT EXISTS accounts_office_enabled BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE company_settings SET accounts_office_enabled = TRUE
 WHERE company_id IN (SELECT id FROM companies WHERE upper(name) LIKE 'KISIIMATT%');

ALTER TABLE payment_requests ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20);   -- MPESA, CHEQUE, MANUAL
ALTER TABLE payment_requests ADD COLUMN IF NOT EXISTS cheque_number  VARCHAR(30);
ALTER TABLE payment_requests ADD COLUMN IF NOT EXISTS cheque_bank    VARCHAR(100);
ALTER TABLE payment_requests ADD COLUMN IF NOT EXISTS cheque_date    DATE;
ALTER TABLE payment_requests ADD COLUMN IF NOT EXISTS paid_on        DATE;
ALTER TABLE payment_requests ADD COLUMN IF NOT EXISTS payment_note   VARCHAR(500);

-- The proof of one payment (receipt, M-Pesa message screenshot, cheque photo). Kept in the database so it is in
-- every backup and survives redeploys. At most one per payment.
CREATE TABLE IF NOT EXISTS payment_evidence (
    id                 BIGSERIAL PRIMARY KEY,
    payment_request_id BIGINT       NOT NULL UNIQUE REFERENCES payment_requests(id),
    file_name          VARCHAR(200) NOT NULL,
    content_type       VARCHAR(100) NOT NULL,
    data               BYTEA        NOT NULL,
    uploaded_by_user_id BIGINT,
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP,
    created_by         VARCHAR(255),
    updated_by         VARCHAR(255)
);

-- The same M-Pesa code or the same cheque cannot prove two payments.
CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_requests_mpesa_code
    ON payment_requests (upper(release_reference)) WHERE payment_method = 'MPESA';
CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_requests_cheque
    ON payment_requests (upper(cheque_bank), upper(cheque_number)) WHERE payment_method = 'CHEQUE';
