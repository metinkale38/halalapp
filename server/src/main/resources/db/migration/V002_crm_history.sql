ALTER TABLE proposals ADD COLUMN IF NOT EXISTS old_json jsonb NULL;

CREATE INDEX IF NOT EXISTS idx_proposals_entity_status_date
    ON proposals (entity_type, status, date DESC);
