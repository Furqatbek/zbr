-- Partner API: external POS systems (Restos and any later ones) calling us.
--
-- Three tables rather than one because the three things have different
-- lifetimes. A partner is a relationship. A key is a credential that gets
-- rotated and revoked without disturbing the relationship. A venue grant is
-- permission over one restaurant, which must survive key rotation — otherwise
-- rotating a credential would silently drop every venue's authorisation.

CREATE TABLE IF NOT EXISTS partners (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(50)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_partners_code ON partners(code);

CREATE TABLE IF NOT EXISTS partner_keys (
    id              BIGSERIAL PRIMARY KEY,
    partner_id      BIGINT       NOT NULL REFERENCES partners(id),
    -- The public half of the credential: not a secret, indexed, and what turns
    -- an incoming key into a single row without scanning the table.
    key_id          VARCHAR(40)  NOT NULL,
    -- SHA-256 of the secret half, hex. Not bcrypt: this is verified on EVERY
    -- partner request, and a deliberately slow hash there is a self-inflicted
    -- rate limit. The secret is 32 random characters from a CSPRNG rather than
    -- a human-chosen password, so there is nothing for a slow hash to defend.
    secret_hash     VARCHAR(64)  NOT NULL,
    environment     VARCHAR(20)  NOT NULL,
    label           VARCHAR(200),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at      TIMESTAMP,
    last_used_at    TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_partner_keys_key_id ON partner_keys(key_id);
CREATE INDEX IF NOT EXISTS idx_partner_keys_partner ON partner_keys(partner_id);

CREATE TABLE IF NOT EXISTS partner_venue_grants (
    id                BIGSERIAL PRIMARY KEY,
    partner_id        BIGINT       NOT NULL REFERENCES partners(id),
    restaurant_id     BIGINT       NOT NULL REFERENCES restaurants(id),
    -- The venue's id in the PARTNER's system. Their calls address venues and
    -- products by their own identifiers, so they never have to store ours.
    external_venue_id VARCHAR(100) NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP
);

-- One grant per venue in each direction. Without the second index two of their
-- venue ids could point at one restaurant of ours, and an update meant for one
-- would land on the other.
CREATE UNIQUE INDEX IF NOT EXISTS idx_partner_grants_restaurant
    ON partner_venue_grants(partner_id, restaurant_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_partner_grants_external
    ON partner_venue_grants(partner_id, external_venue_id);

-- Capabilities are per grant, not per partner: writing into one venue's kitchen
-- must be a separate decision from writing into another's. A grant with no rows
-- here can do nothing, which is what makes a fresh grant read-only by default.
CREATE TABLE IF NOT EXISTS partner_venue_grant_capabilities (
    grant_id    BIGINT      NOT NULL REFERENCES partner_venue_grants(id) ON DELETE CASCADE,
    capability  VARCHAR(40) NOT NULL,
    PRIMARY KEY (grant_id, capability)
);
