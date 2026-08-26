-- Local wall-clock view of a UTC-stored timestamp.
--
-- Every timestamp column in this schema is TIMESTAMP WITHOUT TIME ZONE holding
-- UTC (the JVM and the JDBC connection are both pinned to UTC). Analytics
-- queries grouped on DATE(col) and EXTRACT(HOUR FROM col), which bucket by the
-- UTC calendar: the platform's "day" ran 05:00-05:00 Tashkent and every
-- hour-of-day chart was shifted five hours, so the lunch peak appeared at 16:00.
--
-- business_ts() converts the stored instant to local wall-clock time so DATE(),
-- EXTRACT(HOUR ...) and EXTRACT(DOW ...) all bucket the way an operator reads a
-- clock. Storage is untouched; this only affects how rows are grouped.
--
-- IMMUTABLE is required for use in indexes and lets the planner fold it. That is
-- only sound because the zone is a literal here, not a session setting — which
-- is also why changing the business timezone means a new migration rather than
-- an environment variable. It should agree with app.timezone.

CREATE OR REPLACE FUNCTION business_ts(ts TIMESTAMP)
RETURNS TIMESTAMP
LANGUAGE SQL
IMMUTABLE
PARALLEL SAFE
AS $$
    SELECT ts AT TIME ZONE 'UTC' AT TIME ZONE 'Asia/Tashkent'
$$;

COMMENT ON FUNCTION business_ts(TIMESTAMP) IS
    'UTC-stored timestamp rendered as Asia/Tashkent wall-clock time, for DATE()/EXTRACT() grouping. Keep in sync with app.timezone.';
