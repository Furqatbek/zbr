#!/usr/bin/env bash
#
# Issue a partner's staging credentials.
#
# Run against STAGING. The key it issues is stamped with the environment the
# server is running as, and a staging key is refused by production — which is
# the point: a test order must never print in a real kitchen.
#
#   ADMIN_TOKEN=... ./scripts/onboard-partner.sh RESTOS "Restos" 100 55
#
# Arguments: <partner code> <partner name> <our restaurant id> <their venue id>
#
# The venue id is the one THEY give you. Restos said they would send it once we
# are ready to create the grant, so run steps 1 and 2 first if you do not have
# it yet — the key can be issued before the venue is decided.

set -euo pipefail

BASE="${BASE:-https://staging.zbrr.uz}"
CODE="${1:?partner code, e.g. RESTOS}"
NAME="${2:?partner name, e.g. Restos}"
RESTAURANT_ID="${3:-}"
VENUE_ID="${4:-}"

if [ -z "${ADMIN_TOKEN:-}" ]; then
  echo "ADMIN_TOKEN is required (an admin or platform session token)" >&2
  exit 2
fi

api() {
  curl -sS -X "$1" "$BASE$2" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H 'Content-Type: application/json' \
    ${3:+-d "$3"}
}

say() { printf '\n\033[1m==> %s\033[0m\n' "$1"; }

say "Registering partner $CODE"
# The code must match the external_source stamped on their imported menu items,
# which is what confines them to items they imported themselves. For Restos
# that is RESTOS.
REGISTER=$(api POST /api/v1/admin/partners "{\"code\":\"$CODE\",\"name\":\"$NAME\"}")
echo "$REGISTER"

PARTNER_ID=$(printf '%s' "$REGISTER" | sed -n 's/.*"id":\([0-9]*\).*/\1/p' | head -1)
if [ -z "$PARTNER_ID" ]; then
  echo "Could not read the partner id from the response above." >&2
  echo "If it says the partner already exists, find its id and pass it as PARTNER_ID." >&2
  PARTNER_ID="${PARTNER_ID:?}"
fi

say "Issuing a key for partner $PARTNER_ID"
echo "The secret below is shown ONCE. It is stored only as a digest — if it is"
echo "lost, the only remedy is to issue a new key and revoke this one."
api POST "/api/v1/admin/partners/$PARTNER_ID/keys" \
  "{\"label\":\"$NAME staging, issued $(date +%Y-%m-%d)\"}"

if [ -z "$RESTAURANT_ID" ] || [ -z "$VENUE_ID" ]; then
  say "No venue grant created"
  echo "Pass <restaurant id> and <their venue id> to create one, e.g.:"
  echo "  ADMIN_TOKEN=... $0 $CODE \"$NAME\" 100 55"
  exit 0
fi

say "Granting venue $VENUE_ID -> restaurant $RESTAURANT_ID"
# pushOrders stays FALSE here on purpose. Letting them read and write the menu
# is one decision; sending a restaurant's live orders to someone else's kitchen
# is another, and it should not ride along with the first.
api PUT "/api/v1/admin/partners/$PARTNER_ID/venues" \
  "{\"restaurantId\":$RESTAURANT_ID,\"externalVenueId\":\"$VENUE_ID\",
    \"capabilities\":[\"MENU_WRITE\",\"ORDER_STATUS_WRITE\"],\"pushOrders\":false}"

say "Done"
cat <<EOF

Still to do before orders flow to them, deliberately NOT done here:

  1. Their outbound details — where we POST orders, and the credential they
     issued us:

     curl -X PUT $BASE/api/v1/admin/partners/$PARTNER_ID/outbound \\
       -H "Authorization: Bearer \$ADMIN_TOKEN" -H 'Content-Type: application/json' \\
       -d '{"baseUrl":"https://<their staging host>","apiKey":"<their key>",
            "authHeader":"X-Partner-Key"}'

  2. Switch the venue on, once 1 is done and the payload schema is agreed:

     curl -X PUT $BASE/api/v1/admin/partners/$PARTNER_ID/venues \\
       -H "Authorization: Bearer \$ADMIN_TOKEN" -H 'Content-Type: application/json' \\
       -d '{"restaurantId":$RESTAURANT_ID,"externalVenueId":"$VENUE_ID",
            "capabilities":["MENU_WRITE","ORDER_STATUS_WRITE"],"pushOrders":true}'

EOF
