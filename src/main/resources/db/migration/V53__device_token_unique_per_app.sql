-- One push token row per APP per device, not per device.
--
-- V36 made registration upsert on (user_id, device_id) to stop a rotated token
-- leaving a duplicate row behind. That was right for one app and wrong for
-- three: our apps report the SAME device_id on one phone. Android's ANDROID_ID
-- is scoped to the app signing key and iOS's identifierForVendor to the vendor,
-- so apps published by one developer share both values.
--
-- The effect on anyone running two of our apps against one account — a courier
-- who also orders food, which is most of them: the second app's registration
-- found the first app's row and overwrote device_token and app_id. One row
-- survived where there were two apps, and every push for that person went to
-- whichever app had registered last. Courier job alerts arrived on the customer
-- app; order updates arrived on the courier app mid-delivery.
--
-- V37 added app_id a release later for APNs topics and did not widen this key,
-- which is how the two ended up disagreeing about what a row identifies.
--
-- Note this must be fixed BEFORE app.push.app-ids is configured. With one row
-- holding one app's id, filtering by app would not fix the mixing — it would
-- drop the other app's pushes entirely. Mixed would become missing, which is
-- harder to notice and worse for a courier waiting on a job.

DROP INDEX IF EXISTS uq_user_device_tokens_user_device;

-- COALESCE rather than a bare app_id: Postgres treats NULLs as distinct in a
-- unique index, so legacy rows with no app_id would be free to duplicate --
-- reintroducing the N-copies-of-every-push problem V36 existed to solve.
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_device_tokens_user_device_app
    ON user_device_tokens (user_id, device_id, COALESCE(app_id, ''))
    WHERE device_id IS NOT NULL;

-- Nothing to backfill. The rows that were overwritten lost the other app's
-- token at the moment of collision and it cannot be recovered from here; each
-- app re-registers on next launch and lands in its own row.
