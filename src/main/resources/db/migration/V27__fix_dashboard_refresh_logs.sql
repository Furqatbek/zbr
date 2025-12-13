-- =====================================================
-- V27: Add missing columns to dashboard_refresh_logs
-- =====================================================

ALTER TABLE dashboard_refresh_logs ADD COLUMN IF NOT EXISTS panel_name VARCHAR(50);
ALTER TABLE dashboard_refresh_logs ADD COLUMN IF NOT EXISTS admin_user_id BIGINT;
ALTER TABLE dashboard_refresh_logs ADD COLUMN IF NOT EXISTS query_duration_ms INTEGER;
ALTER TABLE dashboard_refresh_logs ADD COLUMN IF NOT EXISTS cache_hit BOOLEAN DEFAULT FALSE;
ALTER TABLE dashboard_refresh_logs ADD COLUMN IF NOT EXISTS records_returned INTEGER;
ALTER TABLE dashboard_refresh_logs ADD COLUMN IF NOT EXISTS filter_params VARCHAR(1000);
ALTER TABLE dashboard_refresh_logs ADD COLUMN IF NOT EXISTS error_message VARCHAR(500);
ALTER TABLE dashboard_refresh_logs ADD COLUMN IF NOT EXISTS successful BOOLEAN DEFAULT TRUE;

-- Copy data from existing columns to new aliases
UPDATE dashboard_refresh_logs SET panel_name = component WHERE panel_name IS NULL AND component IS NOT NULL;
UPDATE dashboard_refresh_logs SET query_duration_ms = duration_ms WHERE query_duration_ms IS NULL AND duration_ms IS NOT NULL;
UPDATE dashboard_refresh_logs SET error_message = message WHERE error_message IS NULL AND message IS NOT NULL;
UPDATE dashboard_refresh_logs SET successful = success WHERE successful IS NULL AND success IS NOT NULL;
UPDATE dashboard_refresh_logs SET records_returned = records_processed WHERE records_returned IS NULL AND records_processed IS NOT NULL;
