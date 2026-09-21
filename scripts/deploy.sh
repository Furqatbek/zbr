#!/usr/bin/env bash
#
# Deploy the backend. Run from anywhere; it finds the repository itself.
#
#   ./scripts/deploy.sh                 # production (zbrr.uz)
#   ./scripts/deploy.sh --staging       # staging (staging.zbrr.uz)
#   ./scripts/deploy.sh --pull          # git pull the current branch first
#
# WHY THIS EXISTS, and why you must not go back to a bare `docker compose up`:
#
# docker/nginx/conf.d/zbrr.conf declares production's backend in an `upstream`
# block. nginx resolves hostnames in an upstream block ONCE, at startup, and
# caches the address for the lifetime of the process — there is no re-resolution
# and no TTL. `docker compose up -d app` recreates the container, which usually
# gives it a NEW address on the compose network, and nginx keeps connecting to
# the old one. Every request then 502s while the application logs show a clean,
# healthy startup and no incoming traffic at all. That combination is the
# signature of this bug and it is easy to spend an hour on.
#
# Reloading nginx re-reads the configuration and re-resolves the upstream, which
# is why every deploy here ends with a reload. Staging does not need one — its
# server block uses a runtime `resolver` and a variable in proxy_pass, so it
# follows the container by itself. Production keeps the upstream block because
# it buys a keepalive connection pool that matters under real traffic; the
# reload is the price, and this script is how that price stops being forgotten.
#
set -euo pipefail

cd "$(dirname "$0")/.."

TARGET="production"
PULL=false

for arg in "$@"; do
  case "$arg" in
    --staging)    TARGET="staging" ;;
    --production) TARGET="production" ;;
    --pull)       PULL=true ;;
    -h|--help)    sed -n '2,8p' "$0" | sed 's/^# \?//'; exit 0 ;;
    *) echo "unknown option: $arg (try --help)" >&2; exit 2 ;;
  esac
done

if [ "$TARGET" = "staging" ]; then
  COMPOSE=(docker compose -f docker-compose.staging.yml --env-file .env.staging)
  SERVICE="staging-app"
  CONTAINER="zbr-staging-app"
  PORT_VAR="STAGING_APP_HOST_PORT"
  PORT_DEFAULT="8090"
  PUBLIC_URL="https://staging.zbrr.uz/api/v1/restaurants"
  ENV_FILE=".env.staging"
  RELOAD_NGINX=false
else
  COMPOSE=(docker compose)
  SERVICE="app"
  CONTAINER="food-delivery-app"
  PORT_VAR="APP_HOST_PORT"
  PORT_DEFAULT="8080"
  PUBLIC_URL="https://zbrr.uz/api/v1/restaurants"
  ENV_FILE=".env"
  RELOAD_NGINX=true
fi

# Resolve the published port the same way compose does: the shell environment
# wins, then the env file, then the default in docker-compose.yml. Reading only
# the shell would probe 8080 on a host that overrode the port in .env, and the
# readiness check would then fail on a perfectly healthy deploy.
resolve_port() {
  if [ -n "${!PORT_VAR:-}" ]; then
    echo "${!PORT_VAR}"
  elif [ -f "$ENV_FILE" ] && grep -qE "^${PORT_VAR}=" "$ENV_FILE"; then
    grep -E "^${PORT_VAR}=" "$ENV_FILE" | tail -1 | cut -d= -f2- | tr -d '"'"'"' \r'
  else
    echo "$PORT_DEFAULT"
  fi
}

say()  { printf '\n\033[1m==> %s\033[0m\n' "$1"; }
fail() { printf '\n\033[1;31m!! %s\033[0m\n' "$1" >&2; }

# --- Preflight -------------------------------------------------------------
if [ ! -f "$ENV_FILE" ]; then
  fail "$ENV_FILE is missing — refusing to deploy $TARGET without its secrets."
  exit 1
fi

LOCAL_PORT="$(resolve_port)"

if $PULL; then
  branch="$(git rev-parse --abbrev-ref HEAD)"
  # --untracked-files=no is essential, not a shortcut. init-staging.sh CREATES
  # docker/nginx/conf.d/staging.conf on the server by design, so counting
  # untracked files made every deploy on a host with staging fail — and
  # `git stash` does not remove untracked files, so the advice this printed did
  # not work either. A pull cannot silently clobber an untracked file; git
  # refuses and says so.
  if [ -n "$(git status --porcelain --untracked-files=no)" ]; then
    fail "Tracked files have local edits. Commit, stash or drop them before --pull."
    git status --short --untracked-files=no >&2
    exit 1
  fi
  say "Pulling origin/$branch"
  git pull origin "$branch"
fi

say "Deploying $TARGET from $(git rev-parse --short HEAD) ($(git rev-parse --abbrev-ref HEAD))"

# The image the running container was started from. Recorded BEFORE the build,
# because a successful build overwrites the tag and this is then the only handle
# left on the version that is known to work. Printed in the rollback hint below.
PREV_IMAGE="$(docker inspect -f '{{.Image}}' "$CONTAINER" 2>/dev/null || true)"
IMAGE_TAG="$(docker inspect -f '{{.Config.Image}}' "$CONTAINER" 2>/dev/null || true)"
PREV_TAG=""

# --- Keep the running version reachable ------------------------------------
# An image id in a message is not a rollback. The build overwrites :latest, the
# old image becomes dangling, and the next `docker image prune` deletes it — so
# the hint printed on failure can name an image that no longer exists. That is
# not theoretical: production was down for an hour with nothing to go back to,
# because the only surviving zbr-app image WAS the broken one.
#
# A name of its own keeps it. :previous is a tag like any other, which means
# prune leaves it alone and `docker tag zbr-app:previous zbr-app` is the whole
# rollback.
#
# Only from a container that is actually working. Tagging a crash-looping
# container's image as :previous would overwrite the last good one with the
# thing we are trying to escape from — which is exactly how the old hint came
# to point at a broken image on the second attempt.
keep_previous() {
  say "Keeping the running image as :previous"

  if [ -z "$PREV_IMAGE" ] || [ -z "$IMAGE_TAG" ]; then
    echo "     nothing running yet — first deploy, no previous version to keep"
    return 0
  fi

  local running health base
  running="$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null || echo false)"
  health="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' \
            "$CONTAINER" 2>/dev/null || echo unknown)"

  if [ "$running" != "true" ] || { [ "$health" != "healthy" ] && [ "$health" != "none" ]; }; then
    echo "     NOT tagging :previous — the running container is $health (running=$running)."
    echo "     Any existing :previous is left alone; it is the last version known to work."
    return 0
  fi
  [ "$health" = "none" ] && echo "     (no healthcheck on this container — tagging on 'running' alone)"

  # Strip the tag, keeping any registry host:port before the first slash.
  base="$IMAGE_TAG"
  case "${base##*/}" in *:*) base="${base%:*}" ;; esac
  PREV_TAG="${base}:previous"

  docker tag "$PREV_IMAGE" "$PREV_TAG"
  echo "     $PREV_TAG -> ${PREV_IMAGE#sha256:}"
}
keep_previous

# --- Build separately from the restart -------------------------------------
# `up --build` would tear the old container down and only then discover the
# build is broken. Building first means a compile error costs nothing: the
# running version stays up and serving.
say "Building"
"${COMPOSE[@]}" build "$SERVICE"

# --- Repair the images volume ----------------------------------------------
# A named volume is initialised from the image ONLY the first time it is
# created. Hosts that ran the earlier config — where a root-owned bind mount
# shadowed this volume — still have a root-owned directory, and the fixed
# Dockerfile does nothing for them: every upload keeps failing with
# "could not create category directory".
#
# One chown, as root, in a throwaway container on the same volume. Idempotent
# and a no-op once the ownership is already right, so it stays in the normal
# deploy path rather than being a one-off someone has to remember.
# --no-deps so this does not drag Postgres/Redis/RabbitMQ up just to chown.
fix_image_volume_ownership() {
  say "Checking the image storage volume"
  if "${COMPOSE[@]}" run --rm --no-deps --user root --entrypoint sh "$SERVICE" \
       -c 'mkdir -p /app/images && chown -R 10001:10001 /app/images' >/dev/null 2>&1; then
    echo "     /app/images is owned by the application user"
  else
    fail "Could not repair /app/images ownership — image uploads may fail."
    echo "     Run by hand:" >&2
    echo "     ${COMPOSE[*]} run --rm --no-deps --user root --entrypoint sh $SERVICE \\" >&2
    echo "       -c 'chown -R 10001:10001 /app/images'" >&2
  fi
}
fix_image_volume_ownership

say "Restarting $SERVICE"
"${COMPOSE[@]}" up -d "$SERVICE"

# --- Wait for the application itself ---------------------------------------
# Probed on the host loopback port, NOT through nginx: at this point nginx may
# still be pointing at the old container, so going through it would conflate
# "the app failed to start" with "nginx has not been reloaded yet" — the two
# failures this script exists to keep apart.
#
# 200 is expected. 401/403 also prove the app is answering; a bare GET of the
# restaurants collection is not a public route. 000 means nothing is listening.
say "Waiting for the application (up to 3 minutes)"
ready=false
for _ in $(seq 1 36); do
  code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 \
         "http://127.0.0.1:${LOCAL_PORT}/api/v1/restaurants" || true)
  case "$code" in
    200|401|403) ready=true; break ;;
  esac
  sleep 5
done

if ! $ready; then
  fail "The application did not start."
  health="$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$CONTAINER" 2>/dev/null || echo unknown)"
  echo "   container health: $health" >&2
  echo >&2
  echo "   Look here first — a failed Flyway migration and a bad env var both" >&2
  echo "   show up as a startup crash:" >&2
  echo "     ${COMPOSE[*]} logs $SERVICE --tail 80" >&2
  if [ -n "$PREV_TAG" ]; then
    echo >&2
    echo "   To put the previous version back:" >&2
    echo "     docker tag $PREV_TAG $IMAGE_TAG" >&2
    echo "     ${COMPOSE[*]} up -d --no-build $SERVICE" >&2
    echo "   NOTE: this reverts the CODE only. Flyway migrations are not undone," >&2
    echo "   so if the new version added one, the old code runs against the new" >&2
    echo "   schema. That is fine for an additive migration and is not fine for" >&2
    echo "   a destructive one — check what shipped before relying on this." >&2
  else
    echo >&2
    echo "   NO ROLLBACK AVAILABLE: nothing was tagged :previous this run, because" >&2
    echo "   the container was already unhealthy when the deploy started. Check" >&2
    echo "   whether an older one survived:" >&2
    echo "     docker images -a | head" >&2
  fi
  exit 1
fi
echo "    application is answering on 127.0.0.1:${LOCAL_PORT}"

# --- Point nginx at the new container --------------------------------------
if $RELOAD_NGINX; then
  say "Reloading nginx (re-resolves the upstream to the new container)"
  if ! docker compose exec -T nginx nginx -t; then
    fail "nginx configuration is invalid — NOT reloading."
    echo "   The old configuration is still live, so the site is up but is still" >&2
    echo "   routing to the previous container's address. Fix the config, then:" >&2
    echo "     docker compose exec nginx nginx -s reload" >&2
    exit 1
  fi
  docker compose exec -T nginx nginx -s reload
else
  say "Skipping nginx reload — $TARGET resolves its upstream at request time"
fi

# --- Verify from outside ---------------------------------------------------
say "Verifying $PUBLIC_URL"
code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "$PUBLIC_URL" || echo 000)
case "$code" in
  200|401|403)
    printf '    HTTP %s — deploy complete\n\n' "$code"
    ;;
  502|504)
    fail "HTTP $code — the application is up but nginx cannot reach it."
    echo "   The reload should have fixed exactly this. Check that the upstream" >&2
    echo "   name in docker/nginx/conf.d/zbrr.conf still matches the container:" >&2
    echo "     docker compose exec -T nginx tail -5 /var/log/nginx/zbrr.error.log" >&2
    echo "     docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}} {{end}}' $CONTAINER" >&2
    exit 1
    ;;
  *)
    fail "HTTP $code — unexpected."
    echo "   The application answered locally, so this is between the internet" >&2
    echo "   and nginx: DNS, the certificate, or the host firewall." >&2
    exit 1
    ;;
esac
