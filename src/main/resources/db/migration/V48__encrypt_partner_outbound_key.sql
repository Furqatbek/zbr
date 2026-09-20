-- The partner credential we present when calling THEM is now encrypted at rest.
--
-- It is the one secret here that cannot be hashed: we have to replay it on
-- every call, so a digest is no use. It was stored in the clear, which made a
-- database dump a live credential to a partner's production system — the
-- asymmetry being that their key to call US is hashed, and the two sat in the
-- same table looking alike.
--
-- Widened because AES-GCM ciphertext plus a 12-byte IV, base64 and prefixed, is
-- roughly a third longer than the plaintext. 2000 leaves room for a long key
-- without another migration.
ALTER TABLE partners ALTER COLUMN outbound_api_key TYPE VARCHAR(2000);

-- Existing rows stay readable: the converter passes through any value without
-- the enc:v1: prefix. Re-saving a partner's outbound settings rewrites it
-- encrypted, and PartnerAccessService logs a warning for any that still need it.
