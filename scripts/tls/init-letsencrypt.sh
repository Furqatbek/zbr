#!/usr/bin/env bash
#
# One-time TLS bootstrap for the nginx reverse proxy.
#
#   ./scripts/tls/init-letsencrypt.sh you@zbrr.uz
#
# nginx will not start without a certificate, and certbot's webroot challenge
# needs nginx to serve it — so the first certificate is issued with certbot's
# own temporary web server (--standalone) while nginx is stopped. Every renewal
# after that goes through nginx via the webroot challenge, which is why the last
# step here is a renewal dry run: it proves the renewal path works now instead
# of finding out 60 days from now when the certificate actually expires.
#
# Env overrides:
#   DOMAINS="zbrr.uz www.zbrr.uz"   domains on the certificate (first = primary)
#   STAGING=1                       use the Let's Encrypt staging CA (untrusted
#                                   certificate, but no rate limits — use this
#                                   to rehearse)
#
set -euo pipefail

cd "$(dirname "$0")/../.."

EMAIL="${1:-${CERTBOT_EMAIL:-}}"
DOMAINS="${DOMAINS:-zbrr.uz www.zbrr.uz}"
STAGING="${STAGING:-0}"

if [ -z "$EMAIL" ]; then
  echo "usage: $0 <email>            # Let's Encrypt expiry notices go here" >&2
  exit 1
fi

read -r -a DOMAIN_LIST <<<"$DOMAINS"
PRIMARY="${DOMAIN_LIST[0]}"

# certbot names the certificate directory after the FIRST -d argument, and
# docker/nginx/conf.d/zbrr.conf reads /etc/letsencrypt/live/<primary>/. Keep
# them in step or nginx starts with "cannot load certificate".
CERT_DIR="/etc/letsencrypt/live/${PRIMARY}"

DOMAIN_ARGS=()
for d in "${DOMAIN_LIST[@]}"; do DOMAIN_ARGS+=(-d "$d"); done

STAGING_ARG=()
if [ "$STAGING" != "0" ]; then
  echo "!! STAGING mode — the certificate will NOT be trusted by browsers or phones."
  STAGING_ARG=(--staging)
fi

compose() { docker compose "$@"; }

# --- 1. DNS ----------------------------------------------------------------
# Issuance fails if the names do not resolve to this host, and a failed attempt
# still counts against Let's Encrypt's rate limit. Check first.
echo "==> Checking DNS"
MY_IP="$(curl -fsS --max-time 10 https://api.ipify.org || true)"
for d in "${DOMAIN_LIST[@]}"; do
  resolved="$(getent ahostsv4 "$d" 2>/dev/null | awk 'NR==1{print $1}' || true)"
  if [ -z "$resolved" ]; then
    echo "    $d does not resolve — add the A record and wait for propagation." >&2
    exit 1
  fi
  echo "    $d -> $resolved"
  if [ -n "$MY_IP" ] && [ "$resolved" != "$MY_IP" ]; then
    echo "    WARNING: this host looks like $MY_IP. If a CDN sits in front that" >&2
    echo "             is fine; otherwise issuance will fail." >&2
  fi
done

# --- 2. Already have one? --------------------------------------------------
if compose run --rm --entrypoint sh certbot -c "[ -d '$CERT_DIR' ]" >/dev/null 2>&1; then
  echo "==> Certificate for $PRIMARY already exists — nothing to issue."
  echo "    (Renewal is automatic. To start over: docker volume rm zbr_certbot-conf)"
else
  # --- 3. Issue ------------------------------------------------------------
  # nginx owns :80 once it is up, so stop it for the standalone challenge.
  # -p 80:80 is REQUIRED: the compose service declares no ports, so without it
  # the ACME server cannot reach the container and validation times out.
  echo "==> Stopping nginx so certbot can bind :80"
  compose stop nginx >/dev/null 2>&1 || true

  echo "==> Requesting certificate for: ${DOMAIN_LIST[*]}"
  compose run --rm -p 80:80 --entrypoint certbot certbot \
    certonly --standalone \
    "${DOMAIN_ARGS[@]}" \
    "${STAGING_ARG[@]}" \
    --email "$EMAIL" --agree-tos --no-eff-email --non-interactive
fi

# --- 4. Bring the stack up -------------------------------------------------
echo "==> Starting the stack"
compose up -d

# --- 5. Prove renewal works ------------------------------------------------
# The certificate was issued with --standalone but renews with --webroot. This
# dry run exercises the real path (Let's Encrypt staging -> port 80 -> nginx ->
# the shared certbot-www volume) without spending a rate-limited issuance.
echo "==> Waiting for nginx"
for _ in $(seq 1 30); do
  if curl -fsS --max-time 5 -o /dev/null \
      "http://${PRIMARY}/.well-known/acme-challenge/ping" 2>/dev/null \
     || [ "$(curl -s --max-time 5 -o /dev/null -w '%{http_code}' \
             "http://${PRIMARY}/.well-known/acme-challenge/ping" || true)" = "404" ]; then
    break   # 404 is the right answer — nginx is serving the webroot, file absent
  fi
  sleep 2
done

if [ "$(compose ps -q nginx | wc -l)" -eq 0 ] || \
   [ -z "$(compose ps --status running -q nginx 2>/dev/null)" ]; then
  echo "!! nginx is not running. Its logs will say why — the usual cause is that" >&2
  echo "   the certificate directory is not /etc/letsencrypt/live/${PRIMARY}/," >&2
  echo "   which is the path docker/nginx/conf.d/zbrr.conf reads:" >&2
  echo "     docker compose logs nginx" >&2
  exit 1
fi

echo "==> Verifying the renewal path (dry run)"
if compose exec -T certbot certbot renew --webroot -w /var/www/certbot --dry-run; then
  echo
  echo "TLS is live and renewal is verified."
  echo "  API:       https://${PRIMARY}/api/v1/..."
  echo "  WebSocket: wss://${PRIMARY}/ws"
else
  echo >&2
  echo "!! Renewal dry run FAILED. The certificate is installed and the site works," >&2
  echo "   but it will expire in ~90 days unless this is fixed. Usual causes:" >&2
  echo "     - nginx is not serving /.well-known/acme-challenge/ (check zbrr.conf)" >&2
  echo "     - a firewall closed :80 after issuance — it must stay open" >&2
  echo "     - DNS changed" >&2
  exit 1
fi
