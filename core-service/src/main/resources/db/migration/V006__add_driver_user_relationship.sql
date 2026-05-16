-- Link logiflow_drivers to users for authentication
ALTER TABLE logiflow_drivers
    ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users(id) ON DELETE SET NULL;

-- Index for fast lookup by user_id
CREATE INDEX IF NOT EXISTS idx_logiflow_drivers_user_id
    ON logiflow_drivers(tenant_code, user_id);
