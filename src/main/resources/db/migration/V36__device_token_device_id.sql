-- Push tokens are now registered per physical device: the mobile apps send a
-- stable deviceId alongside the raw FCM/APNs token. Registration upserts on
-- (user_id, device_id) so a token rotation updates the existing row instead of
-- creating duplicates — otherwise a vendor receives N copies of every alert.
ALTER TABLE user_device_tokens ADD COLUMN IF NOT EXISTS device_id VARCHAR(200);

-- Collapse pre-existing duplicates (same user+device) down to the newest row so
-- the unique index below can be created on legacy data.
DELETE FROM user_device_tokens t
USING user_device_tokens newer
WHERE t.device_id IS NOT NULL
  AND t.device_id = newer.device_id
  AND t.user_id = newer.user_id
  AND t.id < newer.id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_device_tokens_user_device
    ON user_device_tokens (user_id, device_id)
    WHERE device_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_user_device_tokens_device_id
    ON user_device_tokens (device_id);
