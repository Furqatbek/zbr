#!/usr/bin/env bash
#
# One-screen answer to "what state is everything in right now?"
#
#   ./scripts/status.sh
#
# Read-only: it starts nothing, changes nothing, and prints no secrets — only
# whether each one is set. Safe to run at any time, including during an incident.
#
set -uo pipefail
cd "$(dirname "$0")/.."

PROD="docker compose"
STAG="docker compose -f docker-compose.staging.yml"

hr() { printf '\n\033[1m%s\033[0m\n' "$1"; }
psql_prod() { $PROD exec -T postgres psql -U postgres -d fooddelivery -tAc "$1" 2>/dev/null; }

hr "CONTAINERS"
$PROD ps --format '  prod     {{.Name}}  {{.Status}}' 2>/dev/null | sed 's/food-delivery-//'
$STAG ps --format '  staging  {{.Name}}  {{.Status}}' 2>/dev/null | sed 's/zbr-staging-//' \
  || echo "  staging  (not running)"

hr "REACHABILITY"
for host in zbrr.uz staging.zbrr.uz; do
  # Any HTTP status proves nginx reached the app. 401 is correct here — the
  # restaurant collection endpoint requires auth. 502/000 means it did not.
  code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "https://$host/api/v1/restaurants" || echo 000)
  case "$code" in
    200|401|403) verdict="reachable" ;;
    000)         verdict="NO RESPONSE" ;;
    *)           verdict="check this" ;;
  esac
  printf '  %-18s HTTP %-4s %s\n' "$host" "$code" "$verdict"
done

hr "EFFECTIVE CONFIG (production container)"
$PROD exec -T app printenv 2>/dev/null \
  | grep -E '^(SPRING_PROFILES_ACTIVE|FIREBASE_ENABLED|APNS_ENABLED|APNS_PRODUCTION|SMS_ENABLED|APP_CURRENCY|APP_TIMEZONE|JWT_ACCESS_EXPIRATION|OTP_REVIEW_NUMBERS|CORS_ORIGINS)=' \
  | sort | sed 's/^/  /'
# Presence only — never the value.
for v in JWT_SECRET FIREBASE_CREDENTIALS_BASE64 APNS_KEY_BASE64 SMS_ESKIZ_PASSWORD; do
  if $PROD exec -T app printenv "$v" >/dev/null 2>&1; then
    printf '  %-28s [set]\n' "$v"
  else
    printf '  %-28s [MISSING]\n' "$v"
  fi
done

hr "ACCOUNTS"
psql_prod "SELECT '  ' || id || '  ' || rpad(email, 32) || status FROM users WHERE id <= 5 ORDER BY id;"
echo "  (SUSPENDED = still the committed seed password; ACTIVE = a real one was set)"

hr "DATA"
printf '  restaurants (open)   %s\n' "$(psql_prod "SELECT count(*) FROM restaurants WHERE status='ACTIVE' AND is_open;")"
printf '  restaurants (total)  %s\n' "$(psql_prod "SELECT count(*) FROM restaurants;")"
printf '  orders               %s\n' "$(psql_prod "SELECT count(*) FROM orders;")"
printf '  couriers             %s\n' "$(psql_prod "SELECT count(*) FROM couriers;")"
printf '  device tokens        %s active\n' "$(psql_prod "SELECT count(*) FROM user_device_tokens WHERE is_active;")"
# V28 SEEDS these rows, so "does a row exist" proves nothing — the seeded
# BASE_FEE is 2.00, a USD-shaped placeholder that is nonsense as som. Compare
# the value instead.
printf '  delivery base fee    %s\n' "$(psql_prod "SELECT CASE WHEN setting_value = 2.00 THEN 'NOT SET — still the seeded 2.00 placeholder' ELSE setting_value::text || ' (configured)' END FROM delivery_fee_settings WHERE setting_key='BASE_FEE';" 2>/dev/null || echo 'unknown')"

hr "ERRORS (last hour)"
printf '  production  %s\n' "$($PROD logs app --since 1h 2>/dev/null | grep -c '"level":"ERROR"')"
printf '  staging     %s\n' "$($STAG logs app --since 1h 2>/dev/null | grep -c '"level":"ERROR"')"

hr "CERTIFICATES"
$PROD exec -T certbot certbot certificates 2>/dev/null \
  | grep -E "Certificate Name|Expiry Date" | sed 's/^ */  /'

echo
