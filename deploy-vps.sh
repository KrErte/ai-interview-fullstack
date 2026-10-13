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
#   FRONTEND_PORT=8100
#   FRONTEND_BASE_URL=http://62.171.153.133:8100
#   EOF
#   chmod 600 .env
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
REMOTE
