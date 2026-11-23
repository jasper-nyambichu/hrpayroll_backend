-- V41: where the organisation uses the Accounts Office (Kisiimatt), an advance the Admin approves becomes a payment
-- for the Accounts Office to make (M-Pesa, cheque or cash, with proof). This links the advance to that payment.
-- Additive only; empty for every advance before this and everywhere the Accounts Office is not used.
ALTER TABLE agent_advances ADD COLUMN IF NOT EXISTS payment_request_id BIGINT REFERENCES payment_requests(id);
CREATE INDEX IF NOT EXISTS idx_agent_advances_payment_request ON agent_advances(payment_request_id);
