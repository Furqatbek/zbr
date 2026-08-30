-- Last-online tracking for users.
--
-- last_login_at already existed but answers a different question: it moves only
-- when credentials are exchanged for a token, so a customer who opens the app
-- daily on a still-valid refresh token can look "last seen" weeks ago. Support
-- and the admin panel need "when did this account last do anything", which is
-- what last_seen_at records.
--
-- Stored UTC, like every other timestamp here (the JVM and the JDBC connection
-- are both pinned to UTC — see BusinessTime). NULL means the account has not
-- been active since this migration ran; it is deliberately NOT backfilled from
-- last_login_at, because inventing activity that never happened would be worse
-- than admitting we do not know.

ALTER TABLE users ADD COLUMN last_seen_at TIMESTAMP;

-- Supports the admin "recently active customers" listing. NULLs are the long
-- tail here and are not worth indexing.
CREATE INDEX idx_users_last_seen_at ON users (last_seen_at DESC NULLS LAST);
