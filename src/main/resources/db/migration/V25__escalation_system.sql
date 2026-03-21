-- Auto-Escalation System
-- V25: Automated ticket escalation based on rules

-- Escalation rules table
CREATE TABLE escalation_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    trigger_type VARCHAR(50) NOT NULL,
    condition_json JSONB NOT NULL,
    target_level INT NOT NULL,
    priority INT DEFAULT 0,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Escalation logs for audit trail
CREATE TABLE escalation_logs (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    previous_level INT NOT NULL,
    new_level INT NOT NULL,
    rule_id BIGINT,
    reason VARCHAR(500),
    escalated_by VARCHAR(50) NOT NULL,
    escalated_by_user_id BIGINT,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add escalation level to support tickets if not exists
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'support_tickets'
        AND column_name = 'escalation_level'
    ) THEN
        ALTER TABLE support_tickets ADD COLUMN escalation_level INT NOT NULL DEFAULT 1;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'support_tickets'
        AND column_name = 'last_escalated_at'
    ) THEN
        ALTER TABLE support_tickets ADD COLUMN last_escalated_at TIMESTAMP;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'support_tickets'
        AND column_name = 'sla_breached_at'
    ) THEN
        ALTER TABLE support_tickets ADD COLUMN sla_breached_at TIMESTAMP;
    END IF;
END $$;

-- Indexes for escalation tables
CREATE INDEX idx_er_trigger_type ON escalation_rules(trigger_type);
CREATE INDEX idx_er_active ON escalation_rules(active);
CREATE INDEX idx_el_ticket_id ON escalation_logs(ticket_id);
CREATE INDEX idx_el_created_at ON escalation_logs(created_at);

-- Insert default escalation rules
INSERT INTO escalation_rules (name, trigger_type, condition_json, target_level, priority, description) VALUES
('SLA Breach - No Response', 'SLA_BREACH', '{"noResponseMinutes": 30}', 2, 1, 'Escalate to L2 if SLA breached and no agent response in 30 minutes'),
('High Value Refund', 'REFUND_AMOUNT', '{"minAmount": 50}', 2, 2, 'Escalate to L2 for refund requests over $50'),
('VIP Customer', 'CUSTOMER_VIP', '{}', 2, 3, 'Escalate VIP customer tickets directly to L2'),
('Repeat Complaints', 'REPEAT_COMPLAINT', '{"maxComplaints": 3, "daysPeriod": 30}', 3, 4, 'Escalate to L3 if customer has 3+ complaints in 30 days'),
('Legal Threat', 'LEGAL_THREAT', '{"keywords": ["lawyer", "attorney", "lawsuit", "legal action", "sue", "court"]}', 4, 5, 'Escalate to L4 if ticket contains legal threat keywords'),
('Critical Priority', 'CRITICAL_PRIORITY', '{}', 2, 1, 'Auto-escalate critical priority tickets to L2'),
('Unresolved After 24h', 'TIME_OPEN', '{"hoursOpen": 24}', 2, 2, 'Escalate to L2 if ticket open for more than 24 hours');
