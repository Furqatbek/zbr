# Staging

A second stack on the same VPS at **`https://staging.zbrr.uz`**, for the mobile
teams to build against without touching live orders.

## What it shares with production, and what it doesn't

| | |
|---|---|
| **Shared** | The host, and nginx — one TLS terminator, one port 443 |
| **Not shared** | Database, Redis, RabbitMQ, volumes, images, secrets, JWT signing key |

Sharing nginx is what gives staging a normal `https://` URL with no port number.
Mobile release builds and some corporate networks handle non-standard ports
badly, so a second nginx on `:8443` would have caused more problems than it
solved.

Nothing on staging can affect production data. The one deliberate coupling is
that staging requires production's nginx to be running — which it is, because
that is what serves zbrr.uz.

## Setup (once)

**1. DNS** — an A record for `staging.zbrr.uz` pointing at the same server.

```bash
getent hosts staging.zbrr.uz
```

**2. Secrets:**

```bash
cp .env.staging.example .env.staging
openssl rand -base64 48        # JWT_SECRET — must differ from production's
nano .env.staging
```

**3. Run it:**

```bash
./scripts/staging/init-staging.sh you@zbrr.uz
```

The script checks DNS, refuses to continue if `.env.staging` reuses production's
`JWT_SECRET`, issues a certificate over the **running** nginx (no downtime),
enables the staging server block, validates the nginx config *before* reloading,
and starts the stack.

## Why the nginx config ships disabled

`docker/nginx/conf.d/staging.conf.disabled` is inert until the script copies it
to `staging.conf`. nginx refuses to start when a server block references a
certificate that does not exist, so a `git pull` that dropped an active
`staging.conf` onto a server without the staging certificate would leave
production one container restart away from being down. The script only enables it
after the certificate exists, and reverts it if `nginx -t` fails.

A *reload* with a broken config is safe — nginx keeps the old one. A *restart* is
not. That asymmetry is the whole reason for the `.disabled` suffix.

## A note on `00-tuning.conf`

`server_names_hash_bucket_size 64` lives in `docker/nginx/conf.d/00-tuning.conf`.
nginx sizes its server-name hash from the longest `server_name`, and Alpine's
default bucket of 32 bytes fits `zbrr.uz` and `www.zbrr.uz` but not
`staging.zbrr.uz` — at which point it refuses to load the ENTIRE configuration,
not just the offending block. Adding a longer hostname later will need the same
treatment.

It is an `http`-level directive, so it cannot go inside a `server` block. It
works in `conf.d` because nginx includes those files from inside `http`.

## Daily use

```bash
# start / stop
docker compose -f docker-compose.staging.yml --env-file .env.staging up -d
docker compose -f docker-compose.staging.yml down

# deploy the current branch to staging
git pull && docker compose -f docker-compose.staging.yml --env-file .env.staging up -d --build staging-app

# logs
docker compose -f docker-compose.staging.yml logs -f staging-app

# reset staging data completely (production is untouched)
docker compose -f docker-compose.staging.yml down -v
```

On a 4 GB box, `down` staging when it is not in use — production alone wants
~4.2 GB and staging adds ~1.3 GB.

## What is different about staging's behaviour

These are configured deliberately, not by accident:

| Setting | Staging | Why |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `docker` (no `prod`) | Swagger on, real error messages |
| `JWT_ACCESS_EXPIRATION` | **60000 (60s)** | Exercises token refresh in two minutes instead of an hour → [MOBILE_TOKEN_REFRESH.md](MOBILE_TOKEN_REFRESH.md) |
| `SMS_ENABLED` | `false` | Spends no Eskiz credit, never texts a real person |
| `OTP_REVIEW_NUMBERS` | 3 test numbers | Log in with code `123456`, no SMS |
| `FIREBASE_ENABLED` / `APNS_ENABLED` | `false` | **A staging order must never wake a real courier's phone** |
| `CORS_ORIGINS` | `*` | App teams can point a web build at it from anywhere |

## For the app teams

```
API        https://staging.zbrr.uz/api/v1
WebSocket  wss://staging.zbrr.uz/ws
Swagger    https://staging.zbrr.uz/swagger-ui.html
Login      any of the OTP_REVIEW_NUMBERS, code 123456, no SMS arrives
```

**Access tokens expire after 60 seconds here.** That is intentional and is the
point of the environment: if an app has not implemented refresh, it will break
within a minute, which is exactly the bug we are trying to surface.

### Cautions — read before pointing an app at this

**1. Never ship a build that points at staging.** Put the base URL behind a
build config (`.env.production` / `.env.staging`, an Xcode scheme, a Gradle
flavour) — never a hardcoded constant someone edits by hand and forgets. A
release build on staging takes real customers' orders into a throwaway database,
and a store submission reviewed against staging gets reviewed against demo data
with push disabled.

**2. Switching environments MUST clear stored tokens.** Staging and production
sign with different `JWT_SECRET`s, so a token minted on one is rejected by the
other. If your dev build has an environment toggle, flipping it while a session
is stored produces a stream of 401s that look exactly like a refresh bug and are
not one. Clear both tokens on switch.

**3. Push notifications do not work here, by design.** `FIREBASE_ENABLED` and
`APNS_ENABLED` are hardcoded `false` so a staging test order can never wake a
real courier's phone at 2am. Registering a device token succeeds and nothing is
ever delivered. Push has to be verified against production with a test account —
do not spend a day debugging FCM against staging.

**4. The data is disposable and deliberately fake.** `down -v` wipes it, and the
V2 demo data (Pizza Palace and friends) is present. Do not build test fixtures
you care about, and do not assume a restaurant or order id means the same thing
on production.

**7. The seeded logins do NOT work here either.** SeedAccountGuard is
`@Profile("!test & !dev")`, and staging runs `docker` — so
`admin@fooddelivery.com` / `password` is suspended on staging exactly as it is on
production. To get an admin on staging, set a password hash the same way as the
production bootstrap:

```bash
HASH=$(htpasswd -bnBC 12 "" 'your-staging-password' | tr -d ':\n')
docker compose -f docker-compose.staging.yml exec -T postgres \
  psql -U postgres -d fooddelivery -v h="$HASH" <<'SQL'
UPDATE users SET password_hash = :'h', status = 'ACTIVE'
 WHERE email = 'admin@fooddelivery.com';
SQL
```

The OTP review numbers give you CONSUMER accounts, not an admin — they are a
different thing.

**5. Swagger exists here and NOT in production.** Fine to explore against, but
anything that depends on `/swagger-ui.html` or `/v3/api-docs` being reachable
will fail on production, where the `prod` profile disables both.

**6. SMS never arrives.** Login only works for the numbers in
`OTP_REVIEW_NUMBERS`, with code `123456`. A real phone number will accept the
request and no message will ever come.

## Certificates

Staging gets its own certificate, separate from production's, so reissuing or
losing staging cannot disturb `zbrr.uz`. Production's certbot container renews
both — it renews everything under `/etc/letsencrypt/renewal/`, so no extra job is
needed.

```bash
docker compose exec certbot certbot certificates
```
