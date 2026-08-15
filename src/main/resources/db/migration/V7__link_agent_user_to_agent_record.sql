-- Create the agent business record
INSERT INTO agents (name, charge_type, charge_amount, vat_registered, status, created_at, updated_at)
VALUES ('Test Agent Ltd', 'FIXED_MONTHLY', 5000.00, false, 'ACTIVE', now(), now());

-- Link the existing AGENT user to this agent record
UPDATE users
SET agent_id = (SELECT id FROM agents WHERE name = 'Test Agent Ltd')
WHERE username = 'AGENT';