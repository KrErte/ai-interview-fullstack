#!/usr/bin/env bash
# Käivitab backendi (8080) ja frontend-risk'i (4200) ning avab AI testi.
set -e
cd "$(dirname "$0")"
(cd backend && ./gradlew bootRun) &
BACK=$!
trap 'kill $BACK 2>/dev/null' EXIT
cd frontend-risk
[ -d node_modules ] || npm install
(sleep 25; (xdg-open http://localhost:4200/ai-test || open http://localhost:4200/ai-test) >/dev/null 2>&1) &
npx ng serve --port 4200
