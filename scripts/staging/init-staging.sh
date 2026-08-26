#!/usr/bin/env bash
#
# One-time staging setup. Run from the repository root, on the server, with the
# production stack already up.
#
#   ./scripts/staging/init-staging.sh you@zbrr.uz
#
# Order matters and is the whole reason this is a script: nginx refuses to start
# if a server block references a certificate that does not exist, so the staging
# config ships as staging.conf.disabled and is only enabled AFTER the certificate
# is issued. Enabling it first would leave production one container restart away
# from being down.
#
# The certificate is issued over webroot through the RUNNING nginx, so unlike the
# production bootstrap there is no downtime and nothing is stopped.
#
set -euo pipefail

cd "$(dirname "$0")/../.."

EMAIL="${1:-${CERTBOT_EMAIL:-}}"
DOMAIN="${STAGING_DOMAIN:-staging.zbrr.uz}"
CONF="docker/nginx/conf.d/staging.conf"

if [ -z "$EMAIL" ]; then
  echo "usage: $0 <email>" >&2
  exit 1
fi

if [ ! -f .env.staging ]; then
  echo "!! .env.staging is missing. Copy it and fill in the secrets:" >&2
  echo "     cp .env.staging.example .env.staging" >&2
  echo "   Its JWT_SECRET and DB_PASSWORD MUST differ from production's." >&2
  exit 1
fi

# --- Guard: staging secrets must not equal production's -------------------
if [ -f .env ]; then
  prod_jwt=$(grep -E '^JWT_SECRET=' .env | head -1 | cut -d= -f2-)
  stag_jwt=$(grep -E '^JWT_SECRET=' .env.staging | head -1 | cut -d= -f2-)
  if [ -n "$prod_jwt" ] && [ "$prod_jwt" = "$stag_jwt" ]; then
    echo "!! .env.staging reuses production's JWT_SECRET." >&2
    echo "   A token minted on staging would then be valid on production." >&2
    echo "   Generate a separate one: openssl rand -base64 48" >&2
    exit 1
  fi
fi

echo "==> Checking DNS for $DOMAIN"
resolved="$(getent ahostsv4 "$DOMAIN" 2>/dev/null | awk 'NR==1{print $1}' || true)"
if [ -z "$resolved" ]; then
  echo "    $DOMAIN does not resolve. Add an A record pointing at this server." >&2
  exit 1
fi
echo "    $DOMAIN -> $resolved"

echo "==> Ensuring the production stack (and its nginx) is up"
docker compose up -d nginx >/dev/null

# --- Certificate, over the running nginx ----------------------------------
if docker compose run --rm --entrypoint sh certbot \
     -c "[ -d /etc/letsencrypt/live/$DOMAIN ]" >/dev/null 2>&1; then
  echo "==> Certificate for $DOMAIN already exists"
else
  echo "==> Requesting certificate for $DOMAIN (webroot, no downtime)"
  docker compose run --rm --entrypoint certbot certbot \
    certonly --webroot -w /var/www/certbot \
    -d "$DOMAIN" \
    --email "$EMAIL" --agree-tos --no-eff-email --non-interactive
fi

# --- Enable the server block now that the certificate exists --------------
if [ ! -f "$CONF" ]; then
  echo "==> Enabling $CONF"
  cp "${CONF}.disabled" "$CONF"
fi

echo "==> Validating nginx configuration BEFORE reloading"
if ! docker compose exec -T nginx nginx -t; then
  echo "!! nginx config is invalid — reverting so production is unaffected." >&2
  rm -f "$CONF"
  docker compose exec -T nginx nginx -t >/dev/null 2>&1 || true
  exit 1
fi

docker compose exec -T nginx nginx -s reload
echo "==> nginx reloaded"

# --- Bring up the staging stack -------------------------------------------
echo "==> Starting the staging stack (first run builds the image)"
docker compose -f docker-compose.staging.yml --env-file .env.staging up -d --build

echo "==> Waiting for staging to become healthy"
for _ in $(seq 1 40); do
  code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 \
         "https://$DOMAIN/api/v1/restaurants" || true)
  if [ "$code" = "200" ]; then
    echo
    echo "Staging is live at https://$DOMAIN"
    echo "  API:       https://$DOMAIN/api/v1/..."
    echo "  WebSocket: wss://$DOMAIN/ws"
    echo "  Swagger:   https://$DOMAIN/swagger-ui.html   (on here, off in production)"
    echo "  Access tokens expire in 60s by default — that is deliberate."
    exit 0
  fi
  sleep 5
done

echo "!! Staging did not answer in time. It may still be starting:" >&2
echo "     docker compose -f docker-compose.staging.yml logs -f app" >&2
exit 1
