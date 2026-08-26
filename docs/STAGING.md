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

## Daily use

```bash
# start / stop
docker compose -f docker-compose.staging.yml --env-file .env.staging up -d
docker compose -f docker-compose.staging.yml down

# deploy the current branch to staging
git pull && docker compose -f docker-compose.staging.yml --env-file .env.staging up -d --build app

# logs
docker compose -f docker-compose.staging.yml logs -f app

# reset staging data completely (production is untouched)
docker compose -f docker-compose.staging.yml down -v
```

On a 4 GB box, `down` staging when it is not in use — production alone wants
~4.2 GB and staging adds ~1.3 GB.

## What is different about staging's behaviour

These are configured deliberately, not by accident:

| Setting | Staging | Why |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `docker` (no `prod`) | Swagger on, real error messages, seeded demo accounts usable |
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

## Certificates

Staging gets its own certificate, separate from production's, so reissuing or
losing staging cannot disturb `zbrr.uz`. Production's certbot container renews
both — it renews everything under `/etc/letsencrypt/renewal/`, so no extra job is
needed.

```bash
docker compose exec certbot certbot certificates
```
