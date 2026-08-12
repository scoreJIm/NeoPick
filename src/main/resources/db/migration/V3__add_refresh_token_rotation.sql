ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS family_id VARCHAR(36);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS replaced_by_token VARCHAR(128);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_rt_family ON refresh_tokens(family_id);
