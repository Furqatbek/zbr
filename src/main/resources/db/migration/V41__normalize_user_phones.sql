-- Normalise stored phone numbers to the canonical form (998XXXXXXXXX).
--
-- Two write paths disagreed. The OTP flow normalised and stored
-- "998901234567"; /auth/register and PUT /users/me stored whatever the client
-- sent, so "+998901234567" landed in the same column as a different string. The
-- UNIQUE constraint on users.phone cannot see that those are one number, and
-- neither could the existsByPhone checks, which compared raw input. The result
-- is duplicate accounts for one person: they register in one place, log in by
-- OTP, and arrive at an account with none of their orders or addresses.
--
-- The code fix (PhoneNumbers, applied to every write path) stops new
-- duplicates. This migration cleans up the rows already written.
--
-- IT DELIBERATELY DOES NOT MERGE ANYTHING. Where two accounts normalise to the
-- same number, both are LEFT UNTOUCHED and a warning is raised naming them.
-- Merging is a business decision — which account's orders, addresses, referral
-- credits and reviews survive? — and a migration that guessed would be
-- destroying data on a live platform to satisfy a constraint. See
-- docs/PHONE_DUPLICATES.md for how to resolve them.

CREATE OR REPLACE FUNCTION pg_temp.normalize_phone(p TEXT) RETURNS TEXT AS $$
DECLARE
    d TEXT;
BEGIN
    IF p IS NULL THEN
        RETURN NULL;
    END IF;

    -- Mirrors com.fooddelivery.common.util.PhoneNumbers.normalize. Stripping to
    -- digits also removes the leading '+', which is what the Java does on every
    -- return path.
    d := regexp_replace(p, '[^0-9]', '', 'g');

    IF d = '' THEN
        RETURN p;                                   -- nothing numeric; leave alone
    ELSIF length(d) = 9 THEN
        RETURN '998' || d;                          -- subscriber number alone
    ELSIF length(d) = 10 AND left(d, 1) = '8' THEN
        RETURN '998' || substr(d, 2);               -- local dialling format
    END IF;

    RETURN d;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Only rows whose canonical form is free. The NOT EXISTS covers both shapes of
-- collision: the target already belongs to another account, and two rows that
-- would normalise onto each other.
UPDATE users u
SET phone = pg_temp.normalize_phone(u.phone)
WHERE u.phone IS NOT NULL
  AND u.phone <> pg_temp.normalize_phone(u.phone)
  AND NOT EXISTS (
        SELECT 1 FROM users o
        WHERE o.id <> u.id
          AND o.phone IS NOT NULL
          AND pg_temp.normalize_phone(o.phone) = pg_temp.normalize_phone(u.phone));

-- Anything still un-normalised after that UPDATE is, by definition, a
-- collision. Name them in the startup log rather than failing the migration:
-- refusing to boot would take the platform down over a data problem an operator
-- has to resolve by hand anyway.
DO $$
DECLARE
    r RECORD;
    n INT := 0;
BEGIN
    FOR r IN
        SELECT u.id, u.phone, pg_temp.normalize_phone(u.phone) AS canonical
        FROM users u
        WHERE u.phone IS NOT NULL
          AND u.phone <> pg_temp.normalize_phone(u.phone)
        ORDER BY u.id
    LOOP
        n := n + 1;
        RAISE WARNING
            'phone normalisation SKIPPED: user % has "%" which normalises to "%", already held by another account. These are duplicate accounts for one person — see docs/PHONE_DUPLICATES.md',
            r.id, r.phone, r.canonical;
    END LOOP;

    IF n > 0 THEN
        RAISE WARNING '% account(s) left with a non-canonical phone pending a manual merge.', n;
    END IF;
END $$;

-- pg_temp is session-scoped, so the helper disappears on its own. Keeping a
-- permanent copy would invite it drifting away from the Java it mirrors.
