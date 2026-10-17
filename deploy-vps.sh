#!/usr/bin/env bash
# Deploy main to the shared VPS. Run from your machine (needs SSH key access as root).
#
#   ./deploy-vps.sh                 # default host 62.171.153.133
#   ./deploy-vps.sh other.host.ee
#
# First-time setup on the server (already done on 62.171.153.133):
#   git clone https://github.com/KrErte/ai-interview-fullstack.git /opt/futureproof
#   cd /opt/futureproof && cat > .env <<EOF
#   DB_PASSWORD=$(openssl rand -hex 24)
#   JWT_SECRET=$(openssl rand -hex 48)
#   FRONTEND_PORT=127.0.0.1:8100
#   FRONTEND_BASE_URL=https://careerrisk.ee
#   EOF
#   chmod 600 .env
#
# HTTPS: a shared Caddy (ops/caddy, installed at /opt/caddy) owns ports 80/443 and proxies
# https://careerrisk.ee to 127.0.0.1:8100; lumen moved to 127.0.0.1:8081 behind it.
set -euo pipefail

HOST="${1:-62.171.153.133}"
BRANCH="${BRANCH:-main}"

ssh "root@${HOST}" bash -s -- "$BRANCH" <<'REMOTE'
set -euo pipefail
cd /opt/futureproof
git fetch --quiet origin
git checkout --quiet "$1"
git pull --quiet --ff-only origin "$1"
echo "deploying $(git log --oneline -1)"
# -p futureproof keeps containers, networks and volumes apart from the other apps on this server
docker compose -p futureproof -f docker-compose.yml -f docker-compose.vps.yml up -d --build
docker compose -p futureproof ps
# Smoke check: the public base url must not be localhost. On 2026-10-17 it was, and the sitemap, canonical
# links and team links all pointed to http://localhost:4200 without anyone noticing.
fp="$(sed -n 's/^FRONTEND_PORT=//p' .env)"; fp="${fp:-8100}"
case "$fp" in *:*) ;; *) fp="127.0.0.1:$fp" ;; esac
robots=""
for _ in $(seq 1 40); do
  if robots="$(curl -fsS "http://$fp/robots.txt" 2>/dev/null)"; then break; fi
  robots=""; sleep 3
done
if [ -z "$robots" ]; then echo "ERROR: http://$fp/robots.txt did not answer after the deploy" >&2; exit 1; fi
echo "$robots" | grep -i '^sitemap:' || true
if echo "$robots" | grep -qi 'localhost'; then
  echo "ERROR: robots.txt points to localhost; set FRONTEND_BASE_URL (or APP_PUBLIC_BASE_URL) in .env" >&2
  exit 1
fi
REMOTE
